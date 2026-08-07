package com.coursistant.lms.module.course.content;

import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import com.coursistant.lms.module.course.content.week.repository.CourseWeekMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * Shared course/week visibility + write-permission checks for content sub-modules.
 * Write paths use {@link CourseAuthorizationService} and lock the Course row for Archive races.
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
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private CourseAuthorizationService courseAuthorizationService;

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
     * Locks the course row, ensures visibility + Course Manager, rejects Archived.
     */
    public Course requireCourseManagerWritable(ActorContext actor, Integer courseId) {
        Course locked = lockCourse(courseId);
        courseAuthorizationService.requireCourseManager(actor, courseId);
        requireNotArchived(locked);
        return locked;
    }

    /**
     * Locks course for any content write that must fail closed under Archive races.
     * Caller must still enforce role (manager / TA / event permission).
     */
    public Course requireVisibleCourseWritableLocked(ActorContext actor, Integer courseId) {
        Course locked = lockCourse(courseId);
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        requireNotArchived(locked);
        return locked;
    }

    private Course lockCourse(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        Course locked = courseMapper.selectByIdForUpdate(courseId);
        if (locked == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return locked;
    }

    /**
     * Draft content visibility: SYSTEM_ADMIN, same-tenant TENANT_ADMIN,
     * Active Primary Instructor, Active TA. Students see Published only.
     */
    public boolean canViewDraftContent(ActorContext actor, Integer courseId) {
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        if (actor.isSystemAdmin() || actor.isTenantAdmin()) {
            return true;
        }
        if (!actor.isUser()) {
            return false;
        }
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, actor.getActorId());
        if (enrollment == null || !Boolean.TRUE.equals(enrollment.getActive())) {
            return false;
        }
        String role = enrollment.getCourseRole();
        return CoursePermissionService.ROLE_INSTRUCTOR.equals(role)
                || CoursePermissionService.ROLE_TA.equals(role);
    }

    public CourseWeek requireWeekReadable(ActorContext actor, Integer courseId, Integer weekId) {
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        boolean draftView = canViewDraftContent(actor, courseId);
        CourseWeek week = findWeekInCourse(courseId, weekId);
        if (week == null) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        if (!draftView && !WEEK_STATE_PUBLISHED.equals(week.getState())) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        return week;
    }

    public CourseWeek requireWeekWritable(ActorContext actor, Integer courseId, Integer weekId) {
        requireCourseManagerWritable(actor, courseId);
        CourseWeek week = findWeekInCourse(courseId, weekId);
        if (week == null) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        return week;
    }

    /**
     * Course Manager or any Active TA may upload to an existing week.
     */
    public CourseWeek requireMaterialUpload(ActorContext actor, Integer courseId, Integer weekId) {
        requireVisibleCourseWritableLocked(actor, courseId);
        if (!courseAuthorizationService.isCourseManager(actor, courseId) && !isActiveTa(actor, courseId)) {
            throw new ApiException(ErrorType.FORBIDDEN, "Only Course Manager or Active TA can upload materials");
        }
        CourseWeek week = findWeekInCourse(courseId, weekId);
        if (week == null) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        return week;
    }

    /**
     * Rename / reorder / move: Course Manager only.
     */
    public CourseWeek requireMaterialManage(ActorContext actor, Integer courseId, Integer weekId) {
        return requireWeekWritable(actor, courseId, weekId);
    }

    /**
     * Course Manager may delete any material; Active TA only own uploads.
     */
    public CourseWeek requireMaterialDelete(ActorContext actor, Integer courseId, Integer weekId,
                                            CourseMaterial material) {
        requireVisibleCourseWritableLocked(actor, courseId);
        if (courseAuthorizationService.isCourseManager(actor, courseId)) {
            CourseWeek week = findWeekInCourse(courseId, weekId);
            if (week == null) {
                throw new ApiException(ErrorType.WEEK_NOT_FOUND);
            }
            return week;
        }
        if (!isActiveTa(actor, courseId)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        CourseWeek week = findWeekInCourse(courseId, weekId);
        if (week == null) {
            throw new ApiException(ErrorType.WEEK_NOT_FOUND);
        }
        if (material == null || material.getUploadedBy() == null
                || !material.getUploadedBy().equals(actor.getActorId())) {
            throw new ApiException(ErrorType.FORBIDDEN, "TA can only delete materials they uploaded");
        }
        return week;
    }

    /**
     * Course Event write: Course Manager OR Active TA with canManageCourseEvents.
     */
    public Course requireCourseEventWritable(ActorContext actor, Integer courseId) {
        requireVisibleCourseWritableLocked(actor, courseId);
        if (courseAuthorizationService.isCourseManager(actor, courseId)) {
            return courseMapper.selectById(courseId);
        }
        Enrollment enrollment = requireActiveEnrollment(actor, courseId);
        if (CoursePermissionService.ROLE_TA.equals(enrollment.getCourseRole())
                && Boolean.TRUE.equals(enrollment.getCanManageCourseEvents())) {
            return courseMapper.selectById(courseId);
        }
        throw new ApiException(ErrorType.FORBIDDEN);
    }

    private boolean isActiveTa(ActorContext actor, Integer courseId) {
        if (!actor.isUser()) {
            return false;
        }
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, actor.getActorId());
        return enrollment != null
                && Boolean.TRUE.equals(enrollment.getActive())
                && CoursePermissionService.ROLE_TA.equals(enrollment.getCourseRole());
    }

    private Enrollment requireActiveEnrollment(ActorContext actor, Integer courseId) {
        if (!actor.isUser()) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        Enrollment enrollment = enrollmentMapper.selectByCourseIdAndUserId(courseId, actor.getActorId());
        if (enrollment == null || !Boolean.TRUE.equals(enrollment.getActive())) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        return enrollment;
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
