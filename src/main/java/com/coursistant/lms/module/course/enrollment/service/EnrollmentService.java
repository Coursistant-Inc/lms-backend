package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.dto.AdminBatchEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.BatchEnrollFailure;
import com.coursistant.lms.module.course.enrollment.dto.BatchEnrollResponse;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.dto.DashboardCourseResponse;
import com.coursistant.lms.module.course.enrollment.dto.PromoteTaResponse;
import com.coursistant.lms.module.course.enrollment.dto.UpdateTaPermissionsRequest;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.entity.EnrollmentAuditLog;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentAuditLogMapper;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.service.GroupMembershipService;
import com.coursistant.lms.module.quiz.service.QuizLifecycleHooks;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.util.EmailUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private static final String ROLE_INSTRUCTOR = "Instructor";
    private static final String ROLE_TA = "TA";
    private static final String ROLE_STUDENT = "Student";
    private static final String STATE_ARCHIVED = "Archived";

    private static final String ACTOR_TYPE_USER = "USER";
    private static final String ACTOR_TYPE_ADMIN = "ADMIN";

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private EnrollmentAuditLogMapper enrollmentAuditLogMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private EmailUtil emailUtil;

    @Lazy
    @Resource
    private GroupMembershipService groupMembershipService;

    @Lazy
    @Resource
    private QuizLifecycleHooks quizLifecycleHooks;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Enrolls the course's instructor. Called from {@code CourseService.create} within the
     * same transaction, so any failure here rolls back the course creation as well.
     */
    @Transactional
    public void createInstructorEnrollment(Integer courseId, Integer instructorUserId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(instructorUserId);
        enrollment.setCourseRole(ROLE_INSTRUCTOR);
        enrollment.setCanGrade(true);
        enrollment.setCanPostAnnouncements(true);
        enrollment.setCanManageGroups(true);
        enrollment.setCanManageCourseEvents(true);
        enrollment.setActive(true);
        enrollment.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
        enrollmentMapper.insert(enrollment);

        writeAudit(courseId, instructorUserId, ACTOR_TYPE_USER, instructorUserId, "INSTRUCTOR_ENROLLED", null);
    }

    @Transactional
    public MemberResponse adminEnrollStudent(Integer courseId, Integer userId, Integer adminId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        requireUser(userId);

        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (existing != null) {
            throw new ApiException(ErrorType.CONFLICT, "User is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(userId);
        enrollment.setCourseRole(ROLE_STUDENT);
        enrollment.setCanGrade(false);
        enrollment.setCanPostAnnouncements(false);
        enrollment.setCanManageGroups(false);
        enrollment.setCanManageCourseEvents(false);
        enrollment.setActive(true);
        enrollment.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
        enrollmentMapper.insert(enrollment);

        writeAudit(courseId, userId, ACTOR_TYPE_ADMIN, adminId, "STUDENT_ENROLLED", null);
        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    @Transactional
    public BatchEnrollResponse adminBatchEnroll(Integer courseId, AdminBatchEnrollRequest request, Integer adminId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        if (request == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Request body is required");
        }
        boolean hasUserIds = request.getUserIds() != null && !request.getUserIds().isEmpty();
        boolean hasEmails = request.getEmails() != null && !request.getEmails().isEmpty();
        if (!hasUserIds && !hasEmails) {
            throw new ApiException(ErrorType.PARAM_MISSING, "userIds or emails is required");
        }

        BatchEnrollResponse response = new BatchEnrollResponse();
        Set<Integer> resolvedUserIds = new LinkedHashSet<>();

        if (hasUserIds) {
            for (Integer userId : request.getUserIds()) {
                if (userId == null) {
                    response.getFailed().add(failure(null, null, "PARAM_MISSING", "userId is required"));
                    continue;
                }
                resolvedUserIds.add(userId);
            }
        }
        if (hasEmails) {
            for (String email : request.getEmails()) {
                if (email == null || email.isBlank()) {
                    response.getFailed().add(failure(null, email, "PARAM_MISSING", "email is required"));
                    continue;
                }
                String normalized = email.trim();
                User user = userMapper.selectByEmail(normalized);
                if (user == null) {
                    response.getFailed().add(failure(null, normalized, "USER_NOT_FOUND", "User not found"));
                    continue;
                }
                resolvedUserIds.add(user.getId());
            }
        }

        for (Integer userId : resolvedUserIds) {
            try {
                response.getSucceeded().add(adminEnrollOrReactivateStudent(courseId, userId, adminId));
            } catch (ApiException e) {
                response.getFailed().add(failure(userId, null, e.getErrorType().name(), e.getMessage()));
            } catch (Exception e) {
                response.getFailed().add(failure(userId, null, "INTERNAL_SERVER_ERROR", e.getMessage()));
            }
        }
        return response;
    }

    /**
     * Soft-deactivates an enrollment. Admin may deactivate any role except the sole active Instructor.
     */
    @Transactional
    public MemberResponse adminDeactivateEnrollment(Integer courseId, Integer userId, Integer adminId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            return toMemberResponse(enrollment);
        }
        if (ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())
                && enrollmentMapper.countActiveInstructorsByCourseId(courseId) <= 1) {
            throw new ApiException(ErrorType.CONFLICT, "Cannot deactivate the only active Instructor of this course");
        }
        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setActive(false);
        enrollmentMapper.updateById(patch);
        groupMembershipService.endGroupMembershipsOnEnrollmentDeactivated(
                courseId, userId, GroupMembershipAudit.ACTOR_ADMIN, adminId);
        quizLifecycleHooks.onMembershipIneligible(courseId, userId);
        writeAudit(courseId, userId, ACTOR_TYPE_ADMIN, adminId, "DEACTIVATE", null);
        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    /**
     * Instructor soft-deactivates a Student or TA. Cannot deactivate Instructor.
     */
    @Transactional
    public MemberResponse instructorDeactivateMember(Integer courseId, Integer userId, Integer actorId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        if (ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Instructor cannot deactivate another Instructor");
        }
        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            return toMemberResponse(enrollment);
        }
        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setActive(false);
        enrollmentMapper.updateById(patch);
        groupMembershipService.endGroupMembershipsOnEnrollmentDeactivated(
                courseId, userId, GroupMembershipAudit.ACTOR_USER, actorId);
        quizLifecycleHooks.onMembershipIneligible(courseId, userId);
        writeAudit(courseId, userId, ACTOR_TYPE_USER, actorId, "DEACTIVATE", null);
        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    /**
     * Transfers the sole Instructor role to {@code newInstructorUserId} and demotes the previous Instructor to Student.
     * Caller must already have verified the current instructor and that the course is not archived.
     */
    @Transactional
    public void transferInstructorRole(Integer courseId, Integer oldInstructorUserId, Integer newInstructorUserId, Integer actorId) {
        requireUser(newInstructorUserId);
        User newUser = userMapper.selectById(newInstructorUserId);
        if (!LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(newUser.getLevel())) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "newInstructorId must have platform level INSTRUCTOR");
        }
        if (oldInstructorUserId.equals(newInstructorUserId)) {
            return;
        }

        Enrollment oldEnrollment = requireMember(courseId, oldInstructorUserId);
        if (!ROLE_INSTRUCTOR.equals(oldEnrollment.getCourseRole()) || !Boolean.TRUE.equals(oldEnrollment.getActive())) {
            throw new ApiException(ErrorType.NOT_COURSE_INSTRUCTOR);
        }

        // Demote old instructor first to satisfy uk_enrollment_one_instructor.
        Enrollment demote = new Enrollment();
        demote.setId(oldEnrollment.getId());
        demote.setCourseRole(ROLE_STUDENT);
        demote.setCanGrade(false);
        demote.setCanPostAnnouncements(false);
        demote.setCanManageGroups(false);
        demote.setCanManageCourseEvents(false);
        enrollmentMapper.updateById(demote);

        Enrollment newEnrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, newInstructorUserId);
        if (newEnrollment == null) {
            Enrollment created = new Enrollment();
            created.setCourseId(courseId);
            created.setUserId(newInstructorUserId);
            created.setCourseRole(ROLE_INSTRUCTOR);
            created.setCanGrade(true);
            created.setCanPostAnnouncements(true);
            created.setCanManageGroups(true);
            created.setCanManageCourseEvents(true);
            created.setActive(true);
            created.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
            enrollmentMapper.insert(created);
        } else {
            Enrollment promote = new Enrollment();
            promote.setId(newEnrollment.getId());
            promote.setCourseRole(ROLE_INSTRUCTOR);
            promote.setCanGrade(true);
            promote.setCanPostAnnouncements(true);
            promote.setCanManageGroups(true);
            promote.setCanManageCourseEvents(true);
            promote.setActive(true);
            enrollmentMapper.updateById(promote);
        }

        writeAudit(courseId, newInstructorUserId, ACTOR_TYPE_USER, actorId, "TRANSFER_INSTRUCTOR",
                "{\"fromUserId\":" + oldInstructorUserId + ",\"toUserId\":" + newInstructorUserId + "}");
    }

    public boolean hasActiveInstructorEnrollment(Integer userId) {
        return enrollmentMapper.countActiveInstructorEnrollmentsByUserId(userId) > 0;
    }

    public List<MemberResponse> listMembers(Integer courseId) {
        requireCourse(courseId);
        return enrollmentMapper.selectByCourseId(courseId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PromoteTaResponse promoteToTa(Integer courseId, Integer userId, UpdateTaPermissionsRequest request, Integer actorId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = requireMember(courseId, userId);
        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_ACTIVE);
        }
        if (!ROLE_STUDENT.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.INVALID_ROLE_TRANSITION, "Only a Student can be promoted to TA");
        }

        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setCourseRole(ROLE_TA);
        patch.setCanGrade(request != null && Boolean.TRUE.equals(request.getCanGrade()));
        patch.setCanPostAnnouncements(request != null && Boolean.TRUE.equals(request.getCanPostAnnouncements()));
        patch.setCanManageGroups(request != null && Boolean.TRUE.equals(request.getCanManageGroups()));
        patch.setCanManageCourseEvents(request != null && Boolean.TRUE.equals(request.getCanManageCourseEvents()));
        // A TA grades their former peers, so their own assignment submissions are frozen from
        // here on. Revoking the TA role deliberately does not unfreeze it.
        patch.setAssignmentSubmitFrozen(true);
        enrollmentMapper.updateById(patch);
        quizLifecycleHooks.onMembershipIneligible(courseId, userId);

        writeAudit(courseId, userId, ACTOR_TYPE_USER, actorId, "PROMOTED_TO_TA", toJson(request));
        sendTaChangeEmailBestEffort(userId, course, "promoted to Teaching Assistant");

        PromoteTaResponse response = new PromoteTaResponse();
        response.setMember(toMemberResponse(requireEnrollmentById(enrollment.getId())));
        response.setWarnings(new ArrayList<>());
        return response;
    }

    @Transactional
    public MemberResponse revokeTa(Integer courseId, Integer userId, Integer actorId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = requireMember(courseId, userId);
        if (!ROLE_TA.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.INVALID_ROLE_TRANSITION, "User is not a TA in this course");
        }

        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setCourseRole(ROLE_STUDENT);
        patch.setCanGrade(false);
        patch.setCanPostAnnouncements(false);
        patch.setCanManageGroups(false);
        patch.setCanManageCourseEvents(false);
        enrollmentMapper.updateById(patch);

        writeAudit(courseId, userId, ACTOR_TYPE_USER, actorId, "TA_REVOKED", null);
        sendTaChangeEmailBestEffort(userId, course, "removed as Teaching Assistant");

        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    @Transactional
    public MemberResponse updateTaPermissions(Integer courseId, Integer userId, UpdateTaPermissionsRequest request, Integer actorId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        Enrollment enrollment = requireMember(courseId, userId);
        if (!ROLE_TA.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.INVALID_ROLE_TRANSITION, "User is not a TA in this course");
        }
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }

        Enrollment patch = new Enrollment();
        patch.setId(enrollment.getId());
        patch.setCanGrade(request.getCanGrade());
        patch.setCanPostAnnouncements(request.getCanPostAnnouncements());
        patch.setCanManageGroups(request.getCanManageGroups());
        patch.setCanManageCourseEvents(request.getCanManageCourseEvents());
        enrollmentMapper.updateById(patch);

        writeAudit(courseId, userId, ACTOR_TYPE_USER, actorId, "TA_PERMISSIONS_UPDATED", toJson(request));
        sendTaChangeEmailBestEffort(userId, course, "had their Teaching Assistant permissions updated");

        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    public List<Integer> listActiveCourseIdsForUser(Integer userId) {
        return enrollmentMapper.selectActiveByUserId(userId).stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());
    }

    public List<DashboardCourseResponse> listMyCourses(Integer userId) {
        List<DashboardCourseResponse> result = new ArrayList<>();
        for (Enrollment enrollment : enrollmentMapper.selectActiveByUserId(userId)) {
            Course course = courseMapper.selectById(enrollment.getCourseId());
            if (course == null) {
                continue;
            }
            result.add(toDashboardCourseResponse(enrollment, course));
        }
        return result;
    }

    public int countByCourseId(Integer courseId) {
        return enrollmentMapper.countByCourseId(courseId);
    }

    private MemberResponse adminEnrollOrReactivateStudent(Integer courseId, Integer userId, Integer adminId) {
        requireUser(userId);
        Enrollment existing = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (existing == null) {
            Enrollment enrollment = new Enrollment();
            enrollment.setCourseId(courseId);
            enrollment.setUserId(userId);
            enrollment.setCourseRole(ROLE_STUDENT);
            enrollment.setCanGrade(false);
            enrollment.setCanPostAnnouncements(false);
            enrollment.setCanManageGroups(false);
            enrollment.setCanManageCourseEvents(false);
            enrollment.setActive(true);
            enrollment.setEnrolledAt(LocalDateTime.now(ZoneOffset.UTC));
            enrollmentMapper.insert(enrollment);
            writeAudit(courseId, userId, ACTOR_TYPE_ADMIN, adminId, "STUDENT_ENROLLED", null);
            return toMemberResponse(requireEnrollmentById(enrollment.getId()));
        }
        if (Boolean.TRUE.equals(existing.getActive())) {
            throw new ApiException(ErrorType.CONFLICT, "User is already enrolled in this course");
        }
        Enrollment patch = new Enrollment();
        patch.setId(existing.getId());
        patch.setActive(true);
        if (ROLE_INSTRUCTOR.equals(existing.getCourseRole())) {
            // Keep role; just reactivate.
        } else if (!ROLE_TA.equals(existing.getCourseRole())) {
            patch.setCourseRole(ROLE_STUDENT);
        }
        enrollmentMapper.updateById(patch);
        writeAudit(courseId, userId, ACTOR_TYPE_ADMIN, adminId, "STUDENT_REENROLLED", null);
        return toMemberResponse(requireEnrollmentById(existing.getId()));
    }

    private BatchEnrollFailure failure(Integer userId, String email, String code, String message) {
        BatchEnrollFailure failure = new BatchEnrollFailure();
        failure.setUserId(userId);
        failure.setEmail(email);
        failure.setCode(code);
        failure.setMessage(message);
        return failure;
    }

    private void writeAudit(Integer courseId, Integer targetUserId, String actorType, Integer actorId, String action, String detailJson) {
        EnrollmentAuditLog auditLog = new EnrollmentAuditLog();
        auditLog.setCourseId(courseId);
        auditLog.setTargetUserId(targetUserId);
        auditLog.setActorType(actorType);
        auditLog.setActorId(actorId);
        auditLog.setAction(action);
        auditLog.setDetailJson(detailJson);
        enrollmentAuditLogMapper.insert(auditLog);
    }

    private void sendTaChangeEmailBestEffort(Integer userId, Course course, String actionDescription) {
        try {
            User user = userMapper.selectById(userId);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                String subject = "Course role update: " + course.getTitle();
                String content = "You have been " + actionDescription + " for course " + course.getTitle() + ".";
                emailUtil.sendEmail(user.getEmail(), subject, content);
            }
        } catch (Exception e) {
            log.warn("Failed to send TA change email to user {} for course {}: {}", userId, course.getId(), e.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize audit detail: {}", e.getMessage());
            return null;
        }
    }

    private Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Course id is required");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void requireNotArchived(Course course) {
        if (STATE_ARCHIVED.equals(course.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
    }

    private void requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
    }

    private Enrollment requireMember(Integer courseId, Integer userId) {
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        return enrollment;
    }

    private Enrollment requireEnrollmentById(Integer id) {
        Enrollment enrollment = enrollmentMapper.selectById(id);
        if (enrollment == null) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_FOUND);
        }
        return enrollment;
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
        response.setCreatedAt(enrollment.getCreatedAt());
        response.setUpdatedAt(enrollment.getUpdatedAt());

        User user = userMapper.selectById(enrollment.getUserId());
        if (user != null) {
            response.setUserName(user.getName());
            response.setUserEmail(user.getEmail());
        }
        return response;
    }

    private DashboardCourseResponse toDashboardCourseResponse(Enrollment enrollment, Course course) {
        DashboardCourseResponse response = new DashboardCourseResponse();
        response.setId(course.getId());
        response.setCourseCode(course.getCourseCode());
        response.setTitle(course.getTitle());
        response.setRole(enrollment.getCourseRole());
        return response;
    }
}
