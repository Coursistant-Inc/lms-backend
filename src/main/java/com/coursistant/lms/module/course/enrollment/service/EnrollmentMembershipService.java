package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.course.service.CourseAuditActions;
import com.coursistant.lms.module.course.course.service.CourseAuditService;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.course.service.CourseLifecycleSupport;
import com.coursistant.lms.module.course.enrollment.dto.BatchStudentEnrollResponse;
import com.coursistant.lms.module.course.enrollment.dto.BatchStudentItemResult;
import com.coursistant.lms.module.course.enrollment.dto.MemberPageResponse;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.dto.UpdateTaPermissionsRequest;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.service.GroupMembershipService;
import com.coursistant.lms.module.quiz.service.QuizLifecycleHooks;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Part 3 membership writes: Student/TA with User→Course→Enrollment lock order,
 * course_audit_log only, and soft-withdraw metadata.
 */
@Service
public class EnrollmentMembershipService {

    public static final int MAX_BATCH_SIZE = 100;

    @Resource
    private EnrollmentMapper enrollmentMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private CourseAuthorizationService courseAuthorizationService;
    @Resource
    private CourseAuditService courseAuditService;
    @Resource
    private EnrollmentBatchItemService enrollmentBatchItemService;
    @Lazy
    @Resource
    private GroupMembershipService groupMembershipService;
    @Lazy
    @Resource
    private QuizLifecycleHooks quizLifecycleHooks;

    @Transactional
    public MemberResponse addStudent(ActorContext actor, Integer courseId, Integer userId, String requestId) {
        courseAuthorizationService.requireCourseManager(actor, courseId);
        requireCourseWritable(courseId);
        return upsertStudentLocked(actor, courseId, userId, requestId);
    }

    /**
     * Lock order: User → Course → Enrollment. Used by single Add and REQUIRES_NEW batch items.
     */
    @Transactional
    public MemberResponse upsertStudentLocked(ActorContext actor, Integer courseId, Integer userId, String requestId) {
        User user = lockAndRecheckUserForStudent(userId, courseId);
        Course course = courseMapper.selectByIdForUpdate(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        requireCourseWritable(course);
        recheckUserForStudent(user, course);

        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserIdForUpdate(courseId, userId);
        if (existing == null) {
            Enrollment created = newStudentEnrollment(courseId, userId);
            enrollmentMapper.insert(created);
            MemberResponse resp = toMemberResponse(requireEnrollment(created.getId()));
            audit(actor, course, userId, CourseAuditActions.STUDENT_ADDED, null, snapshot(created), requestId);
            return resp;
        }
        if (Boolean.TRUE.equals(existing.getActive())) {
            if (!CoursePermissionService.ROLE_STUDENT.equals(existing.getCourseRole())) {
                throw new ApiException(ErrorType.CONFLICT,
                        "User already has an active non-Student enrollment; cannot add as Student");
            }
            return toMemberResponse(existing);
        }
        if (!CoursePermissionService.ROLE_STUDENT.equals(existing.getCourseRole())) {
            throw new ApiException(ErrorType.CONFLICT,
                    "Cannot convert inactive TA/Instructor to Student via Student API");
        }
        Map<String, Object> before = snapshot(existing);
        Enrollment patch = new Enrollment();
        patch.setId(existing.getId());
        patch.setActive(true);
        patch.setClearWithdrawn(true);
        enrollmentMapper.updateById(patch);
        Enrollment after = requireEnrollment(existing.getId());
        audit(actor, course, userId, CourseAuditActions.STUDENT_REACTIVATED, before, snapshot(after), requestId);
        return toMemberResponse(after);
    }

    public BatchStudentEnrollResponse batchAddStudents(ActorContext actor, Integer courseId,
                                                       List<Integer> userIds, List<String> emails,
                                                       String requestId) {
        courseAuthorizationService.requireCourseManager(actor, courseId);
        requireCourseWritable(courseId);

        Set<Integer> resolved = new LinkedHashSet<>();
        List<String> unresolvedEmails = new ArrayList<>();
        if (userIds != null) {
            for (Integer id : userIds) {
                if (id != null) {
                    resolved.add(id);
                }
            }
        }
        if (emails != null) {
            for (String email : emails) {
                if (email == null || email.isBlank()) {
                    continue;
                }
                User u = userMapper.selectByEmail(email.trim());
                if (u == null) {
                    unresolvedEmails.add(email.trim());
                } else {
                    resolved.add(u.getId());
                }
            }
        }
        int requested = resolved.size() + unresolvedEmails.size();
        if (requested > MAX_BATCH_SIZE) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Batch size must be at most " + MAX_BATCH_SIZE);
        }

        BatchStudentEnrollResponse response = new BatchStudentEnrollResponse();
        response.setRequestedCount(requested);
        int success = 0;
        int failure = 0;
        for (Integer uid : resolved) {
            BatchStudentItemResult item = new BatchStudentItemResult();
            item.setUserId(uid);
            try {
                MemberResponse member = enrollmentBatchItemService.addStudentItem(actor, courseId, uid, requestId);
                item.setStatus("SUCCESS");
                item.setMember(member);
                success++;
            } catch (ApiException e) {
                item.setStatus("ERROR");
                item.setErrorType(sanitizeBatchError(e.getErrorType()).name());
                item.setMessage(e.getMessage());
                failure++;
            } catch (Exception e) {
                item.setStatus("ERROR");
                item.setErrorType(ErrorType.INTERNAL_SERVER_ERROR.name());
                item.setMessage("Failed to enroll user");
                failure++;
            }
            response.getItems().add(item);
        }
        for (String email : unresolvedEmails) {
            BatchStudentItemResult item = new BatchStudentItemResult();
            item.setStatus("ERROR");
            item.setErrorType(ErrorType.USER_NOT_FOUND.name());
            item.setMessage("User not found");
            response.getItems().add(item);
            failure++;
        }
        response.setSuccessCount(success);
        response.setFailureCount(failure);
        return response;
    }

    @Transactional
    public MemberResponse withdrawStudent(ActorContext actor, Integer courseId, Integer userId, String requestId) {
        courseAuthorizationService.requireCourseManager(actor, courseId);
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            // natural idempotent: nothing to withdraw
            return null;
        }
        Course course = courseMapper.selectByIdForUpdate(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserIdForUpdate(courseId, userId);
        if (existing == null || !Boolean.TRUE.equals(existing.getActive())) {
            return existing == null ? null : toMemberResponse(existing);
        }
        if (!CoursePermissionService.ROLE_STUDENT.equals(existing.getCourseRole())) {
            throw new ApiException(ErrorType.CONFLICT, "Target is not an active Student");
        }
        return softWithdraw(actor, course, existing, CourseAuditActions.STUDENT_WITHDRAWN, requestId, false);
    }

    @Transactional
    public MemberResponse addTa(ActorContext actor, Integer courseId, Integer userId, String requestId) {
        courseAuthorizationService.requireCourseManager(actor, courseId);
        requireCourseWritable(courseId);

        User user = lockAndRecheckUserForTa(userId, courseId);
        Course course = courseMapper.selectByIdForUpdate(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        requireCourseWritable(course);
        recheckUserForTa(user, course);

        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserIdForUpdate(courseId, userId);
        if (existing == null) {
            Enrollment created = newTaEnrollment(courseId, userId);
            enrollmentMapper.insert(created);
            Enrollment after = requireEnrollment(created.getId());
            audit(actor, course, userId, CourseAuditActions.TA_ADDED, null, snapshot(after), requestId);
            return toMemberResponse(after);
        }
        if (Boolean.TRUE.equals(existing.getActive())) {
            if (CoursePermissionService.ROLE_INSTRUCTOR.equals(existing.getCourseRole())) {
                throw new ApiException(ErrorType.CONFLICT, "Primary Instructor cannot be added as TA");
            }
            if (CoursePermissionService.ROLE_STUDENT.equals(existing.getCourseRole())) {
                throw new ApiException(ErrorType.CONFLICT, "Withdraw Student enrollment before adding as TA");
            }
            if (CoursePermissionService.ROLE_TA.equals(existing.getCourseRole())) {
                return toMemberResponse(existing);
            }
        }
        if (CoursePermissionService.ROLE_INSTRUCTOR.equals(existing.getCourseRole())) {
            throw new ApiException(ErrorType.CONFLICT, "Cannot convert inactive Instructor via TA API");
        }
        if (CoursePermissionService.ROLE_STUDENT.equals(existing.getCourseRole())
                && Boolean.TRUE.equals(existing.getActive())) {
            throw new ApiException(ErrorType.CONFLICT, "Withdraw Student enrollment before adding as TA");
        }

        Map<String, Object> before = snapshot(existing);
        boolean roleChange = !CoursePermissionService.ROLE_TA.equals(existing.getCourseRole());
        Enrollment patch = new Enrollment();
        patch.setId(existing.getId());
        patch.setCourseRole(CoursePermissionService.ROLE_TA);
        patch.setCanGrade(false);
        patch.setCanPostAnnouncements(false);
        patch.setCanManageGroups(false);
        patch.setCanManageCourseEvents(false);
        patch.setAssignmentSubmitFrozen(true);
        patch.setActive(true);
        patch.setClearWithdrawn(true);
        enrollmentMapper.updateById(patch);
        Enrollment after = requireEnrollment(existing.getId());
        String action = roleChange
                ? CourseAuditActions.ENROLLMENT_ROLE_CHANGED
                : CourseAuditActions.TA_REACTIVATED;
        audit(actor, course, userId, action, before, snapshot(after), requestId);
        return toMemberResponse(after);
    }

    @Transactional
    public MemberResponse removeTa(ActorContext actor, Integer courseId, Integer userId, String requestId) {
        courseAuthorizationService.requireCourseManager(actor, courseId);
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            return null;
        }
        Course course = courseMapper.selectByIdForUpdate(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserIdForUpdate(courseId, userId);
        if (existing == null || !Boolean.TRUE.equals(existing.getActive())) {
            return existing == null ? null : toMemberResponse(existing);
        }
        if (!CoursePermissionService.ROLE_TA.equals(existing.getCourseRole())) {
            throw new ApiException(ErrorType.CONFLICT, "Target is not an active TA");
        }
        return softWithdraw(actor, course, existing, CourseAuditActions.TA_REMOVED, requestId, false);
    }

    @Transactional
    public MemberResponse patchTaPermissions(ActorContext actor, Integer courseId, Integer userId,
                                             UpdateTaPermissionsRequest request, String requestId) {
        courseAuthorizationService.requireCourseManager(actor, courseId);
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        if (CourseLifecycleSupport.isArchived(course)) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
        userMapper.selectByIdForUpdate(userId);
        courseMapper.selectByIdForUpdate(courseId);
        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserIdForUpdate(courseId, userId);
        if (existing == null || !Boolean.TRUE.equals(existing.getActive())
                || !CoursePermissionService.ROLE_TA.equals(existing.getCourseRole())) {
            throw new ApiException(ErrorType.CONFLICT, "Target must be an active TA");
        }
        Map<String, Object> before = snapshot(existing);
        Enrollment patch = new Enrollment();
        patch.setId(existing.getId());
        if (request != null) {
            if (request.getCanGrade() != null) {
                patch.setCanGrade(request.getCanGrade());
            }
            if (request.getCanPostAnnouncements() != null) {
                patch.setCanPostAnnouncements(request.getCanPostAnnouncements());
            }
            if (request.getCanManageGroups() != null) {
                patch.setCanManageGroups(request.getCanManageGroups());
            }
            if (request.getCanManageCourseEvents() != null) {
                patch.setCanManageCourseEvents(request.getCanManageCourseEvents());
            }
        }
        enrollmentMapper.updateById(patch);
        Enrollment after = requireEnrollment(existing.getId());
        audit(actor, course, userId, CourseAuditActions.TA_PERMISSIONS_UPDATED, before, snapshot(after), requestId);
        return toMemberResponse(after);
    }

    public MemberPageResponse listMembers(ActorContext actor, Integer courseId, String courseRole,
                                          Boolean active, String q, Integer page, Integer size) {
        courseAuthorizationService.requireCourseManager(actor, courseId);
        List<Enrollment> all = enrollmentMapper.selectByCourseId(courseId);
        String roleFilter = courseRole == null || courseRole.isBlank() ? null : courseRole.trim();
        String qNorm = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        List<MemberResponse> filtered = all.stream()
                .filter(e -> roleFilter == null || roleFilter.equals(e.getCourseRole()))
                .filter(e -> active == null || active.equals(e.getActive()))
                .map(this::toMemberResponse)
                .filter(m -> qNorm == null
                        || (m.getUserName() != null && m.getUserName().toLowerCase(Locale.ROOT).contains(qNorm))
                        || (m.getUserEmail() != null && m.getUserEmail().toLowerCase(Locale.ROOT).contains(qNorm)))
                .sorted(Comparator
                        .comparing(MemberResponse::getCourseRole, Comparator.nullsLast(String::compareTo))
                        .thenComparing(MemberResponse::getUserId, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        int pageNum = page == null || page < 0 ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        int from = Math.min(pageNum * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        MemberPageResponse response = new MemberPageResponse();
        response.setItems(new ArrayList<>(filtered.subList(from, to)));
        response.setPage(pageNum);
        response.setSize(pageSize);
        response.setTotal(filtered.size());
        return response;
    }

    /**
     * Soft-withdraw all active TA/Student enrollments for a user (Disable path).
     * Caller must already hold User FOR UPDATE.
     */
    @Transactional
    public void withdrawAllActiveNonInstructorForDisable(ActorContext actor, Integer userId, String requestId) {
        List<Enrollment> actives = enrollmentMapper.selectActiveByUserIdForUpdate(userId);
        for (Enrollment e : actives) {
            if (CoursePermissionService.ROLE_INSTRUCTOR.equals(e.getCourseRole())) {
                throw new ApiException(ErrorType.CONFLICT,
                        "User is still an Active Primary Instructor; reassign before disable");
            }
            Course course = courseMapper.selectByIdForUpdate(e.getCourseId());
            if (course == null) {
                continue;
            }
            softWithdraw(actor, course, e, CourseAuditActions.ENROLLMENT_WITHDRAWN_BY_ACCOUNT_DISABLE,
                    requestId, true);
        }
    }

    private MemberResponse softWithdraw(ActorContext actor, Course course, Enrollment existing,
                                        String action, String requestId, boolean accountDisable) {
        Map<String, Object> before = snapshot(existing);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Enrollment patch = new Enrollment();
        patch.setId(existing.getId());
        patch.setActive(false);
        patch.setWithdrawnAt(now);
        patch.setWithdrawnByActorType(actor.getActorType());
        patch.setWithdrawnByActorId(actor.getActorId());
        enrollmentMapper.updateById(patch);

        String groupActor = ActorContext.ACTOR_ADMIN.equals(actor.getActorType())
                ? GroupMembershipAudit.ACTOR_ADMIN
                : GroupMembershipAudit.ACTOR_USER;
        groupMembershipService.endGroupMembershipsOnEnrollmentDeactivated(
                course.getId(), existing.getUserId(), groupActor, actor.getActorId());
        quizLifecycleHooks.onMembershipIneligible(course.getId(), existing.getUserId());

        Enrollment after = requireEnrollment(existing.getId());
        audit(actor, course, existing.getUserId(), action, before, snapshot(after), requestId);
        return toMemberResponse(after);
    }

    private User lockAndRecheckUserForStudent(Integer userId, Integer courseId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        recheckUserForStudent(user, course);
        return user;
    }

    private User lockAndRecheckUserForTa(Integer userId, Integer courseId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        recheckUserForTa(user, course);
        return user;
    }

    private void recheckUserForStudent(User user, Course course) {
        if (user.getStatus() != null && !AccountStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new ApiException(ErrorType.ACCOUNT_DISABLED);
        }
        if (!RoleEnum.USER.name().equals(user.getRole())) {
            throw new ApiException(ErrorType.ENROLLMENT_ROLE_FORBIDDEN);
        }
        if (!LevelEnum.STUDENT.level.equalsIgnoreCase(user.getLevel())) {
            throw new ApiException(ErrorType.LEVEL_ENROLLMENT_MISMATCH);
        }
        if (user.getTenantId() == null || !user.getTenantId().equals(course.getTenantId())) {
            throw new ApiException(ErrorType.TENANT_MISMATCH);
        }
        requireActiveTenant(course.getTenantId());
    }

    private void recheckUserForTa(User user, Course course) {
        if (user.getStatus() != null && !AccountStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new ApiException(ErrorType.ACCOUNT_DISABLED);
        }
        if (!RoleEnum.USER.name().equals(user.getRole())) {
            throw new ApiException(ErrorType.ENROLLMENT_ROLE_FORBIDDEN);
        }
        if (!LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(user.getLevel())) {
            throw new ApiException(ErrorType.LEVEL_ENROLLMENT_MISMATCH);
        }
        if (user.getTenantId() == null || !user.getTenantId().equals(course.getTenantId())) {
            throw new ApiException(ErrorType.TENANT_MISMATCH);
        }
        requireActiveTenant(course.getTenantId());
        if (Objects.equals(course.getInstructorId(), user.getId())) {
            throw new ApiException(ErrorType.CONFLICT, "Primary Instructor cannot be added as TA");
        }
    }

    private void requireActiveTenant(Integer tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ApiException(ErrorType.TENANT_NOT_FOUND);
        }
        if (tenant.getStatus() != null && !AccountStatus.ACTIVE.name().equals(tenant.getStatus())) {
            throw new ApiException(ErrorType.FORBIDDEN, "Tenant is not active");
        }
    }

    private void requireCourseWritable(Integer courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        requireCourseWritable(course);
    }

    private void requireCourseWritable(Course course) {
        if (CourseLifecycleSupport.isArchived(course)) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
    }

    private Enrollment newStudentEnrollment(Integer courseId, Integer userId) {
        Enrollment e = new Enrollment();
        e.setCourseId(courseId);
        e.setUserId(userId);
        e.setCourseRole(CoursePermissionService.ROLE_STUDENT);
        e.setCanGrade(false);
        e.setCanPostAnnouncements(false);
        e.setCanManageGroups(false);
        e.setCanManageCourseEvents(false);
        e.setActive(true);
        e.setAssignmentSubmitFrozen(false);
        e.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
        return e;
    }

    private Enrollment newTaEnrollment(Integer courseId, Integer userId) {
        Enrollment e = new Enrollment();
        e.setCourseId(courseId);
        e.setUserId(userId);
        e.setCourseRole(CoursePermissionService.ROLE_TA);
        e.setCanGrade(false);
        e.setCanPostAnnouncements(false);
        e.setCanManageGroups(false);
        e.setCanManageCourseEvents(false);
        e.setActive(true);
        e.setAssignmentSubmitFrozen(true);
        e.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
        return e;
    }

    private ErrorType sanitizeBatchError(ErrorType type) {
        if (type == ErrorType.TENANT_MISMATCH) {
            return ErrorType.USER_NOT_FOUND;
        }
        return type;
    }

    private void audit(ActorContext actor, Course course, Integer targetUserId, String action,
                       Object before, Object after, String requestId) {
        courseAuditService.write(actor, course.getId(), course.getTenantId(), action,
                CourseAuditActions.TARGET_ENROLLMENT, targetUserId, before, after, requestId);
    }

    private Map<String, Object> snapshot(Enrollment e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("courseId", e.getCourseId());
        m.put("userId", e.getUserId());
        m.put("courseRole", e.getCourseRole());
        m.put("active", e.getActive());
        m.put("canGrade", e.getCanGrade());
        m.put("canPostAnnouncements", e.getCanPostAnnouncements());
        m.put("canManageGroups", e.getCanManageGroups());
        m.put("canManageCourseEvents", e.getCanManageCourseEvents());
        m.put("assignmentSubmitFrozen", e.getAssignmentSubmitFrozen());
        m.put("withdrawnAt", e.getWithdrawnAt());
        m.put("withdrawnByActorType", e.getWithdrawnByActorType());
        m.put("withdrawnByActorId", e.getWithdrawnByActorId());
        return m;
    }

    private Enrollment requireEnrollment(Integer id) {
        Enrollment e = enrollmentMapper.selectById(id);
        if (e == null) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        return e;
    }

    private MemberResponse toMemberResponse(Enrollment enrollment) {
        MemberResponse response = new MemberResponse();
        response.setId(enrollment.getId());
        response.setCourseId(enrollment.getCourseId());
        response.setUserId(enrollment.getUserId());
        response.setCourseRole(enrollment.getCourseRole());
        response.setCanGrade(enrollment.getCanGrade());
        response.setCanPostAnnouncements(enrollment.getCanPostAnnouncements());
        response.setCanManageGroups(enrollment.getCanManageGroups());
        response.setCanManageCourseEvents(enrollment.getCanManageCourseEvents());
        response.setActive(enrollment.getActive());
        response.setEnrolledAt(enrollment.getEnrolledAt());
        response.setJoinedAt(enrollment.getEnrolledAt());
        response.setWithdrawnAt(enrollment.getWithdrawnAt());
        response.setCreatedAt(enrollment.getCreatedAt());
        response.setUpdatedAt(enrollment.getUpdatedAt());
        User user = userMapper.selectById(enrollment.getUserId());
        if (user != null) {
            response.setUserName(user.getName());
            response.setUserEmail(user.getEmail());
            response.setLevel(user.getLevel());
        }
        return response;
    }
}
