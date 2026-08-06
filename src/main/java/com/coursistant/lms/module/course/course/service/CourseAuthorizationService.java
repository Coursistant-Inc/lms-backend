package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * Canonical course authorization entry for Part 1+.
 * <p>
 * Coexistence with {@link CoursePermissionService}: this service is the normative API for
 * newly wired endpoints. Unwired call sites may keep using CPS. Do not double-check the same
 * request path with both services using conflicting ErrorTypes.
 */
@Service
public class CourseAuthorizationService {

    public static final String PERM_CAN_GRADE = "canGrade";
    public static final String PERM_CAN_POST_ANNOUNCEMENTS = "canPostAnnouncements";
    public static final String PERM_CAN_MANAGE_GROUPS = "canManageGroups";
    public static final String PERM_CAN_MANAGE_COURSE_EVENTS = "canManageCourseEvents";

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private EnrollmentMapper enrollmentMapper;
    @Resource
    private UserMapper userMapper;

    public Course requireVisibleCourse(ActorContext actor, Integer courseId) {
        Course course = loadCourseOrNotFound(courseId);
        if (actor.isSystemAdmin()) {
            return course;
        }
        if (actor.isTenantAdmin()) {
            if (actor.getTenantId() == null || !actor.getTenantId().equals(course.getTenantId())) {
                throw new ApiException(ErrorType.COURSE_NOT_FOUND);
            }
            return course;
        }
        if (actor.isUser()) {
            if (actor.getTenantId() == null || !actor.getTenantId().equals(course.getTenantId())) {
                throw new ApiException(ErrorType.COURSE_NOT_FOUND);
            }
            Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, actor.getActorId());
            if (enrollment == null || !Boolean.TRUE.equals(enrollment.getActive())) {
                throw new ApiException(ErrorType.COURSE_NOT_FOUND);
            }
            return course;
        }
        throw new ApiException(ErrorType.COURSE_NOT_FOUND);
    }

    /**
     * Course Manager = SYSTEM_ADMIN | same-tenant TENANT_ADMIN | Active Primary Instructor.
     * TA is never a Course Manager.
     */
    public Course requireCourseManager(ActorContext actor, Integer courseId) {
        Course course = requireVisibleCourse(actor, courseId);
        if (actor.isSystemAdmin()) {
            return course;
        }
        if (actor.isTenantAdmin()) {
            return course;
        }
        if (actor.isUser() && isActivePrimaryInstructor(courseId, actor.getActorId())) {
            return course;
        }
        throw new ApiException(ErrorType.FORBIDDEN);
    }

    public Course requirePrimaryInstructor(ActorContext actor, Integer courseId) {
        Course course = requireVisibleCourse(actor, courseId);
        if (!actor.isUser() || !isActivePrimaryInstructor(courseId, actor.getActorId())) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        return course;
    }

    public Enrollment requireActiveTeachingMember(ActorContext actor, Integer courseId) {
        requireVisibleCourse(actor, courseId);
        Enrollment enrollment = requireActiveEnrollmentRow(courseId, actor.getActorId());
        String role = enrollment.getCourseRole();
        if (!CoursePermissionService.ROLE_INSTRUCTOR.equals(role)
                && !CoursePermissionService.ROLE_TA.equals(role)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        return enrollment;
    }

    public Enrollment requireActiveStudent(ActorContext actor, Integer courseId) {
        requireVisibleCourse(actor, courseId);
        Enrollment enrollment = requireActiveEnrollmentRow(courseId, actor.getActorId());
        if (!CoursePermissionService.ROLE_STUDENT.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        return enrollment;
    }

    /**
     * TA configurable permission gate. Part 1 provides the method surface only.
     */
    public Enrollment requireTaPermission(ActorContext actor, Integer courseId, String permission) {
        requireVisibleCourse(actor, courseId);
        Enrollment enrollment = requireActiveEnrollmentRow(courseId, actor.getActorId());
        if (!CoursePermissionService.ROLE_TA.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        boolean allowed = switch (permission == null ? "" : permission) {
            case PERM_CAN_GRADE -> Boolean.TRUE.equals(enrollment.getCanGrade());
            case PERM_CAN_POST_ANNOUNCEMENTS -> Boolean.TRUE.equals(enrollment.getCanPostAnnouncements());
            case PERM_CAN_MANAGE_GROUPS -> Boolean.TRUE.equals(enrollment.getCanManageGroups());
            case PERM_CAN_MANAGE_COURSE_EVENTS -> Boolean.TRUE.equals(enrollment.getCanManageCourseEvents());
            default -> false;
        };
        if (!allowed) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        return enrollment;
    }

    public User requireSameTenantUser(Course course, Integer targetUserId) {
        if (course == null || targetUserId == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null || target.getTenantId() == null
                || !target.getTenantId().equals(course.getTenantId())) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return target;
    }

    public boolean isCourseManager(ActorContext actor, Integer courseId) {
        try {
            requireCourseManager(actor, courseId);
            return true;
        } catch (ApiException e) {
            return false;
        }
    }

    private Course loadCourseOrNotFound(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return course;
    }

    private boolean isActivePrimaryInstructor(Integer courseId, Integer userId) {
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        return enrollment != null
                && Boolean.TRUE.equals(enrollment.getActive())
                && CoursePermissionService.ROLE_INSTRUCTOR.equals(enrollment.getCourseRole());
    }

    private Enrollment requireActiveEnrollmentRow(Integer courseId, Integer userId) {
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null || !Boolean.TRUE.equals(enrollment.getActive())) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        return enrollment;
    }
}
