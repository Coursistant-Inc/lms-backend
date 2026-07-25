package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
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

    /**
     * Returns the caller's enrollment for the course, requiring it to exist and be active.
     */
    public Enrollment requireActiveEnrollment(Integer courseId, Integer userId) {
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
