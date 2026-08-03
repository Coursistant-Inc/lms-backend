package com.coursistant.lms.module.course.content;

import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import com.coursistant.lms.module.course.content.week.repository.CourseWeekMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Shared course/week visibility + write-permission checks for the week/material
 * content sub-modules. Depends only on mappers (not the week/material services)
 * so both {@code CourseWeekService} and {@code CourseMaterialService} can depend
 * on this without a circular service graph.
 */
@Service
public class CourseContentAccessService {

    private static final String STATE_ARCHIVED = "Archived";
    public static final String WEEK_STATE_DRAFT = "Draft";
    public static final String WEEK_STATE_PUBLISHED = "Published";

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CourseWeekMapper courseWeekMapper;

    @Resource
    private CoursePermissionService coursePermissionService;

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
        if (STATE_ARCHIVED.equals(course.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
    }

    /**
     * Requires the caller to be the active Instructor of the (existing, any-state) course
     * and the course to not be archived. Returns the course.
     */
    public Course requireCourseWritable(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireInstructor(courseId, userId);
        requireNotArchived(course);
        return course;
    }

    /**
     * Resolves whether the caller may view Draft content for this course: true for
     * platform Admin or the active course Instructor. Other enrolled roles
     * (TA / Student) see Published only. Non-admin callers must be actively enrolled.
     */
    public boolean resolveInstructorView(HttpServletRequest request, Integer courseId, Integer userId) {
        if (coursePermissionService.isSystemAdmin(request)) {
            return true;
        }
        coursePermissionService.requireActiveEnrollment(courseId, userId);
        return coursePermissionService.isInstructor(courseId, userId);
    }

    /**
     * Loads a week and enforces read visibility: Admin and Instructors see any state;
     * everyone else sees Published only. Draft weeks are reported as WEEK_NOT_FOUND
     * to callers without that visibility, so their existence is never leaked.
     */
    public CourseWeek requireWeekReadable(HttpServletRequest request, Integer courseId, Integer weekId, Integer userId) {
        requireCourse(courseId);
        boolean instructorView = resolveInstructorView(request, courseId, userId);
        CourseWeek week = findWeekInCourse(courseId, weekId);
        if (week == null) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        if (!instructorView && !WEEK_STATE_PUBLISHED.equals(week.getState())) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        return week;
    }

    /**
     * Requires the caller to be the course Instructor, the course to not be archived,
     * and the week to exist within the course (any state). Used by week CRUD and
     * Instructor-only material ops (rename / reorder / move).
     */
    public CourseWeek requireWeekWritable(Integer courseId, Integer weekId, Integer userId) {
        requireCourseWritable(courseId, userId);
        CourseWeek week = findWeekInCourse(courseId, weekId);
        if (week == null) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        return week;
    }

    /**
     * Instructor or active TA may upload materials to an existing week when the course
     * is not archived.
     */
    public CourseWeek requireMaterialUpload(Integer courseId, Integer weekId, Integer userId) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireActiveEnrollment(courseId, userId);
        if (!coursePermissionService.isInstructor(courseId, userId)
                && !coursePermissionService.isTa(courseId, userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only course Instructor or TA can upload materials");
        }
        requireNotArchived(course);
        CourseWeek week = findWeekInCourse(courseId, weekId);
        if (week == null) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        return week;
    }

    /**
     * Instructor may delete any material; TA may delete only materials they uploaded.
     */
    public CourseWeek requireMaterialDelete(Integer courseId, Integer weekId, Integer userId,
                                            CourseMaterial material) {
        CourseWeek week = requireMaterialUpload(courseId, weekId, userId);
        if (coursePermissionService.isInstructor(courseId, userId)) {
            return week;
        }
        if (material == null || material.getUploadedBy() == null
                || !material.getUploadedBy().equals(userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "TA can only delete materials they uploaded");
        }
        return week;
    }

    private CourseWeek findWeekInCourse(Integer courseId, Integer weekId) {
        if (weekId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Week id is required");
        }
        CourseWeek week = courseWeekMapper.selectById(weekId);
        if (week == null || !courseId.equals(week.getCourseId())) {
            return null;
        }
        return week;
    }
}
