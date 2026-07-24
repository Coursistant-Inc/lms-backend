package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionMapper;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupSet;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupSetMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Course/group-set access checks for the group module.
 */
@Service
public class GroupAccessService {

    private static final String STATE_ARCHIVED = "Archived";

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private GroupSetMapper groupSetMapper;

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private CoursePermissionService coursePermissionService;

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

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
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        if (STATE_ARCHIVED.equals(course.getState()) || course.getArchivedAt() != null) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
    }

    public Course requireCourseMember(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireActiveEnrollment(courseId, userId);
        return course;
    }

    public Course requireCanManageGroups(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireCanManageGroups(courseId, userId);
        return course;
    }

    public Course requireCanManageGroupsWritable(Integer courseId, Integer userId) {
        Course course = requireCanManageGroups(courseId, userId);
        requireNotArchived(course);
        return course;
    }

    public GroupSet requireGroupSetInCourse(Integer courseId, Integer groupSetId) {
        if (groupSetId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Group set id is required");
        }
        GroupSet groupSet = groupSetMapper.selectById(groupSetId);
        if (groupSet == null || !courseId.equals(groupSet.getCourseId())) {
            throw new ApiException(ErrorType.GROUP_SET_NOT_FOUND);
        }
        return groupSet;
    }

    public CourseGroup requireGroupInSet(Integer courseId, Integer groupSetId, Integer groupId) {
        if (groupId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Group id is required");
        }
        CourseGroup group = courseGroupMapper.selectById(groupId);
        if (group == null
                || !courseId.equals(group.getCourseId())
                || !groupSetId.equals(group.getGroupSetId())) {
            throw new ApiException(ErrorType.GROUP_NOT_FOUND);
        }
        return group;
    }

    public boolean isManager(Integer courseId, Integer userId) {
        return coursePermissionService.canManageGroups(courseId, userId);
    }

    public boolean isStudentSelfServiceOpen(GroupSet groupSet, LocalDateTime now) {
        if (groupSet == null) {
            return false;
        }
        if (Boolean.TRUE.equals(groupSet.getLocked())) {
            return false;
        }
        if (groupSet.getJoinOpensAt() != null && now.isBefore(groupSet.getJoinOpensAt())) {
            return false;
        }
        if (groupSet.getJoinClosesAt() != null && now.isAfter(groupSet.getJoinClosesAt())) {
            return false;
        }
        return true;
    }

    /**
     * Student Join/Leave/Switch gate: locked and join-window rules.
     */
    public void assertStudentSelfServiceAllowed(GroupSet groupSet) {
        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(groupSet.getLocked())) {
            throw new ApiException(ErrorType.GROUP_LOCKED);
        }
        if (groupSet.getJoinOpensAt() != null && now.isBefore(groupSet.getJoinOpensAt())) {
            throw new ApiException(ErrorType.GROUP_WINDOW_CLOSED);
        }
        if (groupSet.getJoinClosesAt() != null && now.isAfter(groupSet.getJoinClosesAt())) {
            throw new ApiException(ErrorType.GROUP_WINDOW_CLOSED);
        }
    }

    /**
     * True when the group has at least one submitted version on any Group assignment
     * (set-scoped in practice because only linked Group assignments write group submissions).
     */
    public boolean hasAcademicHold(Integer courseId, Integer groupId) {
        if (groupId == null) {
            return false;
        }
        return assignmentSubmissionMapper.countVersionsByGroupId(groupId) > 0;
    }

    public void assertNoAcademicHold(Integer courseId, Integer groupId) {
        if (hasAcademicHold(courseId, groupId)) {
            throw new ApiException(ErrorType.GROUP_ACADEMIC_HOLD);
        }
    }
}
