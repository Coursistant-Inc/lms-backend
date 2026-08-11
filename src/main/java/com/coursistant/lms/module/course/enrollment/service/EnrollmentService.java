package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.dto.AdminBatchEnrollRequest;
import com.coursistant.lms.module.course.enrollment.dto.BatchEnrollFailure;
import com.coursistant.lms.module.course.enrollment.dto.BatchEnrollResponse;
import com.coursistant.lms.module.course.enrollment.dto.MemberResponse;
import com.coursistant.lms.module.course.enrollment.dto.DashboardCourseResponse;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.service.GroupMembershipService;
import com.coursistant.lms.module.quiz.service.QuizLifecycleHooks;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
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

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private UserMapper userMapper;

    @Lazy
    @Resource
    private GroupMembershipService groupMembershipService;

    @Lazy
    @Resource
    private QuizLifecycleHooks quizLifecycleHooks;

    /**
     * Enrolls the course's instructor. Called from {@code CourseService.create} within the
     * same transaction, so any failure here rolls back the course creation as well.
     */
    @Transactional
    public void createInstructorEnrollment(Integer courseId, Integer instructorUserId) {
        requireUserForCourseRole(instructorUserId, ROLE_INSTRUCTOR);
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
        // Part 3: enrollment mutations audit via course_audit_log (CourseService create / reassign).
    }

    @Transactional
    public MemberResponse adminEnrollStudent(Integer courseId, Integer userId, Integer adminId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        requireUserForCourseRole(userId, ROLE_STUDENT);

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

        // Part 3: new enrollment audits go to course_audit_log via EnrollmentMembershipService.
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
        return toMemberResponse(requireEnrollmentById(enrollment.getId()));
    }

    /**
     * Transfers the sole Instructor role to {@code newInstructorUserId} and demotes the previous Instructor to Student.
     * Caller must already have verified the current instructor and that the course is not archived.
     */
    @Transactional
    public void transferInstructorRole(Integer courseId, Integer oldInstructorUserId, Integer newInstructorUserId, Integer actorId) {
        requireUserForCourseRole(newInstructorUserId, ROLE_INSTRUCTOR);
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

    public List<Integer> listActiveCourseIdsForUser(Integer userId) {
        return enrollmentMapper.selectActiveByUserId(userId).stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());
    }

    public List<DashboardCourseResponse> listMyCourses(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (user.getTenantId() == null) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Persisted user.tenantId is null");
        }
        Integer userTenantId = user.getTenantId();
        List<DashboardCourseResponse> result = new ArrayList<>();
        for (Enrollment enrollment : enrollmentMapper.selectActiveByUserId(userId)) {
            Course course = courseMapper.selectById(enrollment.getCourseId());
            if (course == null) {
                continue;
            }
            if (course.getTenantId() == null || !userTenantId.equals(course.getTenantId())) {
                log.error("MyCourses cross-tenant filtered userId={} courseId={} userTenantId={} courseTenantId={}",
                        userId, course.getId(), userTenantId, course.getTenantId());
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
        if (RoleEnum.TENANT_ADMIN.name().equals(user.getRole())
                || LevelEnum.NOT_APPLICABLE.level.equalsIgnoreCase(user.getLevel())) {
            throw new ApiException(ErrorType.ENROLLMENT_ROLE_FORBIDDEN);
        }
    }

    private void requireUserForCourseRole(Integer userId, String courseRole) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (RoleEnum.TENANT_ADMIN.name().equals(user.getRole())
                || LevelEnum.NOT_APPLICABLE.level.equalsIgnoreCase(user.getLevel())) {
            throw new ApiException(ErrorType.ENROLLMENT_ROLE_FORBIDDEN);
        }
        String expectedLevel = ROLE_INSTRUCTOR.equals(courseRole)
                ? LevelEnum.INSTRUCTOR.level
                : LevelEnum.STUDENT.level;
        if (user.getLevel() == null || !expectedLevel.equalsIgnoreCase(user.getLevel())) {
            throw new ApiException(ErrorType.LEVEL_ENROLLMENT_MISMATCH);
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
