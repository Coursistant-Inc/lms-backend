package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.group.entity.GroupSet;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.course.group.repository.GroupSetMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupAccessServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private GroupSetMapper groupSetMapper;

    @Mock
    private CourseGroupMapper courseGroupMapper;

    @Mock
    private CoursePermissionService coursePermissionService;

    @InjectMocks
    private GroupAccessService groupAccessService;

    @Test
    void requireCourseMember_nonMember_throwsNotCourseMember() {
        Course course = activeCourse(1);
        when(courseMapper.selectById(1)).thenReturn(course);
        doThrow(new ApiException(ErrorType.NOT_COURSE_MEMBER))
                .when(coursePermissionService).requireActiveEnrollment(1, 99);

        ApiException ex = assertThrows(ApiException.class,
                () -> groupAccessService.requireCourseMember(1, 99));
        assertEquals(ErrorType.NOT_COURSE_MEMBER, ex.getErrorType());
    }

    @Test
    void requireCanManageGroups_withoutPermission_throwsAccessDenied() {
        Course course = activeCourse(1);
        when(courseMapper.selectById(1)).thenReturn(course);
        doThrow(new ApiException(ErrorType.ACCESS_DENIED, "Not permitted to manage groups"))
                .when(coursePermissionService).requireCanManageGroups(1, 50);

        ApiException ex = assertThrows(ApiException.class,
                () -> groupAccessService.requireCanManageGroups(1, 50));
        assertEquals(ErrorType.ACCESS_DENIED, ex.getErrorType());
        verify(coursePermissionService).requireCanManageGroups(1, 50);
    }

    @Test
    void requireCanManageGroupsWritable_archived_throwsCourseArchived() {
        Course course = activeCourse(1);
        course.setState("Archived");
        course.setArchivedAt(LocalDateTime.now());
        when(courseMapper.selectById(1)).thenReturn(course);

        ApiException ex = assertThrows(ApiException.class,
                () -> groupAccessService.requireCanManageGroupsWritable(1, 10));
        assertEquals(ErrorType.COURSE_ARCHIVED, ex.getErrorType());
    }

    @Test
    void assertStudentSelfServiceAllowed_locked_throwsGroupLocked() {
        GroupSet groupSet = new GroupSet();
        groupSet.setLocked(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> groupAccessService.assertStudentSelfServiceAllowed(groupSet));
        assertEquals(ErrorType.GROUP_LOCKED, ex.getErrorType());
    }

    @Test
    void assertStudentSelfServiceAllowed_windowClosed_throwsGroupWindowClosed() {
        GroupSet groupSet = new GroupSet();
        groupSet.setLocked(false);
        groupSet.setJoinClosesAt(LocalDateTime.now().minusDays(1));

        ApiException ex = assertThrows(ApiException.class,
                () -> groupAccessService.assertStudentSelfServiceAllowed(groupSet));
        assertEquals(ErrorType.GROUP_WINDOW_CLOSED, ex.getErrorType());
    }

    private Course activeCourse(Integer id) {
        Course course = new Course();
        course.setId(id);
        course.setState("Active");
        course.setTenantId(1);
        return course;
    }
}
