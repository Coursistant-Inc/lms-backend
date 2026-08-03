package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.AuthzService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Course/assignment visibility and write-permission checks for the assignment module.
 */
@Service
public class AssignmentAccessService {

    public static final String STATE_DRAFT = "Draft";
    public static final String STATE_PUBLISHED = "Published";
    public static final String COURSE_STATE_ARCHIVED = "Archived";
    public static final String SUBMISSION_TYPE_INDIVIDUAL = "Individual";
    public static final String SUBMISSION_TYPE_GROUP = "Group";

    /** Staff may keep grading an archived course for this many days after {@code archivedAt}. */
    public static final int ARCHIVE_GRADING_DAYS = 30;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private CoursePermissionService coursePermissionService;

    @Resource
    private AuthzService authzService;

    public Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Course id is required");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return course;
    }

    public void requireNotArchived(Course course) {
        if (COURSE_STATE_ARCHIVED.equals(course.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
    }

    public Course requireCourseWritable(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireInstructor(courseId, userId);
        requireNotArchived(course);
        return course;
    }

    public Enrollment requireActiveMember(Integer courseId, Integer userId) {
        return coursePermissionService.requireActiveEnrollment(courseId, userId);
    }

    public boolean isStaffViewer(HttpServletRequest request, Integer courseId, Integer userId) {
        // Administrative read for SYSTEM_ADMIN (any tenant) and TENANT_ADMIN (same tenant)
        if (coursePermissionService.isSystemAdmin(request)) {
            return true;
        }
        if (authzService.isTenantAdmin(request)) {
            Course course = requireCourse(courseId);
            authzService.requireTenantAdminOrSystem(request, course.getTenantId());
            return true;
        }
        Enrollment enrollment = coursePermissionService.requireActiveEnrollment(courseId, userId);
        String role = enrollment.getCourseRole();
        return CoursePermissionService.ROLE_INSTRUCTOR.equals(role)
                || CoursePermissionService.ROLE_TA.equals(role);
    }

    public boolean canGrade(Integer courseId, Integer userId) {
        return coursePermissionService.canGrade(courseId, userId);
    }

    public void requireCanGrade(Integer courseId, Integer userId) {
        requireActiveMember(courseId, userId);
        if (!canGrade(courseId, userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Not permitted to grade assignments");
        }
    }

    public void requireCanGrade(HttpServletRequest request, Integer courseId, Integer userId) {
        Object role = request.getAttribute("userRole");
        if (RoleEnum.SYSTEM_ADMIN.name().equals(role) || RoleEnum.TENANT_ADMIN.name().equals(role)) {
            throw new ApiException(ErrorType.FORBIDDEN, "Admins cannot perform daily grading");
        }
        requireCanGrade(courseId, userId);
    }

    public void requireInstructor(Integer courseId, Integer userId) {
        coursePermissionService.requireInstructor(courseId, userId);
    }

    /**
     * Loads assignment in course. Draft is visible to Staff/Admin only;
     * Students receive ASSIGNMENT_NOT_FOUND (no leak).
     */
    public Assignment requireAssignmentReadable(HttpServletRequest request, Integer courseId,
                                                Integer assignmentId, Integer userId) {
        requireCourse(courseId);
        boolean staffView = isStaffViewer(request, courseId, userId);
        Assignment assignment = assignmentMapper.selectByCourseIdAndId(courseId, assignmentId);
        if (assignment == null) {
            throw new ApiException(ErrorType.ASSIGNMENT_NOT_FOUND);
        }
        if (!staffView && !STATE_PUBLISHED.equals(assignment.getState())) {
            throw new ApiException(ErrorType.ASSIGNMENT_NOT_FOUND);
        }
        return assignment;
    }

    public Assignment requireAssignmentConfigurable(Integer courseId, Integer assignmentId, Integer userId) {
        requireCourseWritable(courseId, userId);
        Assignment assignment = assignmentMapper.selectByCourseIdAndId(courseId, assignmentId);
        if (assignment == null) {
            throw new ApiException(ErrorType.ASSIGNMENT_NOT_FOUND);
        }
        return assignment;
    }

    public void requireCanSubmit(Integer courseId, Integer userId) {
        Enrollment enrollment = requireActiveMember(courseId, userId);
        // The freeze is checked before the role so a student promoted to TA is told why they are
        // blocked, rather than getting the generic "students only" message.
        if (Boolean.TRUE.equals(enrollment.getAssignmentSubmitFrozen())) {
            throw new ApiException(ErrorType.SUBMISSION_FROZEN);
        }
        if (!CoursePermissionService.ROLE_STUDENT.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only students can submit assignments");
        }
    }

    /**
     * Whether the member's own submissions are frozen (set when a student is promoted to TA).
     */
    public boolean isSubmitFrozen(Integer courseId, Integer userId) {
        return Boolean.TRUE.equals(requireActiveMember(courseId, userId).getAssignmentSubmitFrozen());
    }

    public boolean isCourseArchived(Course course) {
        return COURSE_STATE_ARCHIVED.equals(course.getState());
    }

    /**
     * Students may only submit into a live course; archiving closes submission immediately.
     */
    public Course requireStudentSubmitContext(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        requireNotArchived(course);
        requireCanSubmit(courseId, userId);
        return course;
    }

    /**
     * Grading stays open for {@value #ARCHIVE_GRADING_DAYS} days after a course is archived so
     * staff can finish marking; after that the course is read-only for grades too.
     */
    public Course requireGradingWritable(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        requireCanGrade(courseId, userId);
        if (!isGradingWritable(course)) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED,
                    "Grading closed " + ARCHIVE_GRADING_DAYS + " days after the course was archived");
        }
        return course;
    }

    /**
     * Release / Retract are Instructor-only (TA with canGrade may PUT grades but not publish them).
     */
    public Course requireReleaseWritable(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        requireInstructor(courseId, userId);
        if (!isGradingWritable(course)) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED,
                    "Grading closed " + ARCHIVE_GRADING_DAYS + " days after the course was archived");
        }
        return course;
    }

    /**
     * Student-only surfaces (my-grades, assignment summaries). Instructor/TA must not use them.
     */
    public void requireStudentMember(Integer courseId, Integer userId) {
        Enrollment enrollment = requireActiveMember(courseId, userId);
        if (!CoursePermissionService.ROLE_STUDENT.equals(enrollment.getCourseRole())) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only students can access this endpoint");
        }
    }

    public boolean isGradingWritable(Course course) {
        if (!isCourseArchived(course)) {
            return true;
        }
        LocalDateTime deadline = gradingWritableUntil(course);
        return deadline != null && !LocalDateTime.now(ZoneOffset.UTC).isAfter(deadline);
    }

    /**
     * The instant after which grading is closed, or null while the course is not archived.
     */
    public LocalDateTime gradingWritableUntil(Course course) {
        if (!isCourseArchived(course) || course.getArchivedAt() == null) {
            return null;
        }
        return course.getArchivedAt().plusDays(ARCHIVE_GRADING_DAYS);
    }
}
