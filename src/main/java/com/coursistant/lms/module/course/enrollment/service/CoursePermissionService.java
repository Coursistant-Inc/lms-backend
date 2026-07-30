package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Centralizes course-role permission checks derived from the caller's active
 * {@link Enrollment} for a given course.
 */
@Service
public class CoursePermissionService {

    public static final String ROLE_INSTRUCTOR = "Instructor";
    public static final String ROLE_TA = "TA";
    public static final String ROLE_STUDENT = "Student";

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * For non-admin callers: if the user's tenant does not match the course tenant,
     * throw {@link ErrorType#COURSE_NOT_FOUND} (anti-enumeration).
     * Admins are exempt.
     */
    public void requireUserTenantMatchesCourse(Integer courseId, Integer userId, boolean admin) {
        if (admin) {
            return;
        }
        if (courseId == null || userId == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        if (user.getTenantId() == null || course.getTenantId() == null) {
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Persisted tenantId is null");
        }
        if (!user.getTenantId().equals(course.getTenantId())) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
    }

    public void requireUserTenantMatchesCourse(HttpServletRequest request, Integer courseId, Integer userId) {
        requireUserTenantMatchesCourse(courseId, userId, isAdmin(request));
    }

    /**
     * Returns the caller's enrollment for the course, requiring it to exist and be active.
     */
    public Enrollment requireActiveEnrollment(Integer courseId, Integer userId) {
        // USER paths: cross-tenant course access is indistinguishable from missing course.
        requireUserTenantMatchesCourse(courseId, userId, false);
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null) {
            throw new ApiException(ErrorType.NOT_COURSE_MEMBER);
        }
        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            throw new ApiException(ErrorType.ENROLLMENT_NOT_ACTIVE);
        }
        return enrollment;
    }

    /**
     * Requires the caller to be the (active) Instructor of the course.
     */
    public Enrollment requireInstructor(Integer courseId, Integer userId) {
        Enrollment enrollment = requireActiveEnrollment(courseId, userId);
        if (!ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.NOT_COURSE_INSTRUCTOR);
        }
        return enrollment;
    }

    /**
     * Requires the caller to be able to manage course events: Instructor, or a TA
     * with {@code canManageCourseEvents} granted.
     */
    public void requireCanManageCourseEvents(Integer courseId, Integer userId) {
        Enrollment enrollment = requireActiveEnrollment(courseId, userId);
        boolean allowed = ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())
                || (ROLE_TA.equals(enrollment.getCourseRole()) && Boolean.TRUE.equals(enrollment.getCanManageCourseEvents()));
        if (!allowed) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Not permitted to manage course events");
        }
    }

    public boolean isInstructor(Integer courseId, Integer userId) {
        return hasActiveRole(courseId, userId, ROLE_INSTRUCTOR);
    }

    public boolean isTa(Integer courseId, Integer userId) {
        return hasActiveRole(courseId, userId, ROLE_TA);
    }

    public boolean isStudent(Integer courseId, Integer userId) {
        return hasActiveRole(courseId, userId, ROLE_STUDENT);
    }

    public boolean canGrade(Integer courseId, Integer userId) {
        Enrollment enrollment = findActiveEnrollment(courseId, userId);
        if (enrollment == null) {
            return false;
        }
        if (ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())) {
            return true;
        }
        return ROLE_TA.equals(enrollment.getCourseRole()) && Boolean.TRUE.equals(enrollment.getCanGrade());
    }

    public boolean canPostAnnouncements(Integer courseId, Integer userId) {
        Enrollment enrollment = findActiveEnrollment(courseId, userId);
        if (enrollment == null) {
            return false;
        }
        if (ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())) {
            return true;
        }
        return ROLE_TA.equals(enrollment.getCourseRole()) && Boolean.TRUE.equals(enrollment.getCanPostAnnouncements());
    }

    /**
     * Requires Instructor, or TA with {@code canPostAnnouncements}.
     */
    public void requireCanPostAnnouncements(Integer courseId, Integer userId) {
        if (!canPostAnnouncements(courseId, userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Not permitted to post announcements");
        }
    }

    /**
     * Mutate (edit/delete) an announcement: Instructor may mutate any; author may mutate own
     * only while still {@link #canPostAnnouncements}.
     */
    public void requireCanMutateAnnouncement(Integer courseId, Integer userId, Integer authorUserId) {
        if (isInstructor(courseId, userId)) {
            return;
        }
        if (userId != null && userId.equals(authorUserId) && canPostAnnouncements(courseId, userId)) {
            return;
        }
        throw new ApiException(ErrorType.ACCESS_DENIED, "Not permitted to modify this announcement");
    }

    public boolean canManageGroups(Integer courseId, Integer userId) {
        Enrollment enrollment = findActiveEnrollment(courseId, userId);
        if (enrollment == null) {
            return false;
        }
        if (ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())) {
            return true;
        }
        return ROLE_TA.equals(enrollment.getCourseRole()) && Boolean.TRUE.equals(enrollment.getCanManageGroups());
    }

    /**
     * Requires Instructor, or TA with {@code canManageGroups}.
     */
    public void requireCanManageGroups(Integer courseId, Integer userId) {
        if (!canManageGroups(courseId, userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Not permitted to manage groups");
        }
    }

    public boolean canManageCourseEvents(Integer courseId, Integer userId) {
        Enrollment enrollment = findActiveEnrollment(courseId, userId);
        if (enrollment == null) {
            return false;
        }
        if (ROLE_INSTRUCTOR.equals(enrollment.getCourseRole())) {
            return true;
        }
        return ROLE_TA.equals(enrollment.getCourseRole()) && Boolean.TRUE.equals(enrollment.getCanManageCourseEvents());
    }

    /**
     * Only active Students may submit assignments (Instructor/TA are excluded).
     */
    public boolean canSubmitAssignments(Integer courseId, Integer userId) {
        return isStudent(courseId, userId);
    }

    /**
     * Only active Students may take quizzes (Instructor/TA are excluded).
     */
    public boolean canTakeQuizzes(Integer courseId, Integer userId) {
        return isStudent(courseId, userId);
    }

    /**
     * Whether the given user is an active Student in the course, i.e. eligible to receive grades.
     */
    public boolean isGradableStudent(Integer courseId, Integer userId) {
        return isStudent(courseId, userId);
    }

    /**
     * Reads the {@code userRole} request attribute set by the JWT interceptor and
     * returns whether the caller is a platform Admin.
     */
    public boolean isAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("userRole");
        return RoleEnum.ADMIN.name().equals(role);
    }

    private boolean hasActiveRole(Integer courseId, Integer userId, String role) {
        Enrollment enrollment = findActiveEnrollment(courseId, userId);
        return enrollment != null && role.equals(enrollment.getCourseRole());
    }

    private Enrollment findActiveEnrollment(Integer courseId, Integer userId) {
        if (courseId == null || userId == null) {
            return null;
        }
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, userId);
        if (enrollment == null || !Boolean.TRUE.equals(enrollment.getActive())) {
            return null;
        }
        return enrollment;
    }
}
