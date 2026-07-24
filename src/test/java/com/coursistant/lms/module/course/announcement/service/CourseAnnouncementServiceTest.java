package com.coursistant.lms.module.course.announcement.service;

import com.coursistant.lms.module.course.announcement.dto.AnnouncementResponse;
import com.coursistant.lms.module.course.announcement.dto.CreateAnnouncementRequest;
import com.coursistant.lms.module.course.announcement.entity.CourseAnnouncement;
import com.coursistant.lms.module.course.announcement.repository.CourseAnnouncementMapper;
import com.coursistant.lms.module.course.announcement.repository.CourseAnnouncementReadMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAnnouncementServiceTest {

    @Mock
    private CourseAnnouncementMapper courseAnnouncementMapper;

    @Mock
    private CourseAnnouncementReadMapper courseAnnouncementReadMapper;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CoursePermissionService coursePermissionService;

    @Mock
    private AnnouncementNotificationService announcementNotificationService;

    @InjectMocks
    private CourseAnnouncementService courseAnnouncementService;

    @Test
    void create_notificationFailure_stillPersistsAnnouncement() {
        Course course = new Course();
        course.setId(17);
        course.setState("Active");
        when(courseMapper.selectById(17)).thenReturn(course);
        doNothing().when(coursePermissionService).requireCanPostAnnouncements(17, 10);

        User user = new User();
        user.setId(10);
        user.setName("Teacher");
        when(userMapper.selectById(10)).thenReturn(user);

        when(courseAnnouncementMapper.insert(any(CourseAnnouncement.class))).thenAnswer(invocation -> {
            CourseAnnouncement a = invocation.getArgument(0);
            a.setId(99);
            return 1;
        });
        when(courseAnnouncementMapper.selectById(99)).thenAnswer(invocation -> {
            CourseAnnouncement a = new CourseAnnouncement();
            a.setId(99);
            a.setCourseId(17);
            a.setTitle("Hello");
            a.setBodyHtml("Body");
            a.setAuthorUserId(10);
            a.setAuthorName("Teacher");
            return a;
        });

        // Mirror AnnouncementNotificationService.afterCommit: run then swallow failures.
        doAnswer(invocation -> {
            try {
                ((Runnable) invocation.getArgument(0)).run();
            } catch (Exception ignored) {
                // notification must not fail post
            }
            return null;
        }).when(announcementNotificationService).afterCommit(any(Runnable.class));
        doThrow(new RuntimeException("notification db down"))
                .when(announcementNotificationService).notifyAnnouncementPosted(any(CourseAnnouncement.class));

        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("Hello");
        request.setBody("Body");

        AnnouncementResponse response = courseAnnouncementService.create(17, 10, request);

        assertNotNull(response);
        assertEquals(99, response.getId());
        assertEquals("Hello", response.getTitle());
        verify(courseAnnouncementMapper).insert(any(CourseAnnouncement.class));
    }

    @Test
    void delete_withoutConfirm_throwsConfirmRequired() {
        Course course = new Course();
        course.setId(17);
        course.setState("Active");
        when(courseMapper.selectById(17)).thenReturn(course);

        ApiException ex = assertThrows(ApiException.class,
                () -> courseAnnouncementService.delete(17, 1, 10, false));
        assertEquals(ErrorType.ANNOUNCEMENT_DELETE_CONFIRM_REQUIRED, ex.getErrorType());
        verify(courseAnnouncementMapper, never()).deleteById(any());
    }

    @Test
    void getById_missing_throwsAnnouncementGone() {
        Course course = new Course();
        course.setId(17);
        course.setState("Active");
        when(courseMapper.selectById(17)).thenReturn(course);
        when(courseAnnouncementMapper.selectById(404)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> courseAnnouncementService.getById(17, 404, 10));
        assertEquals(ErrorType.ANNOUNCEMENT_GONE, ex.getErrorType());
        assertEquals("Content no longer available", ex.getMessage());
    }

    @Test
    void create_schedulesAfterCommitNotification() {
        Course course = new Course();
        course.setId(17);
        course.setState("Active");
        when(courseMapper.selectById(17)).thenReturn(course);
        doNothing().when(coursePermissionService).requireCanPostAnnouncements(17, 10);

        User user = new User();
        user.setId(10);
        user.setUsername("teach");
        when(userMapper.selectById(10)).thenReturn(user);

        when(courseAnnouncementMapper.insert(any(CourseAnnouncement.class))).thenAnswer(invocation -> {
            CourseAnnouncement a = invocation.getArgument(0);
            a.setId(5);
            return 1;
        });
        CourseAnnouncement persisted = new CourseAnnouncement();
        persisted.setId(5);
        persisted.setCourseId(17);
        persisted.setTitle("T");
        persisted.setBodyHtml("B");
        persisted.setAuthorUserId(10);
        persisted.setAuthorName("teach");
        when(courseAnnouncementMapper.selectById(5)).thenReturn(persisted);
        doNothing().when(announcementNotificationService).afterCommit(any());

        CreateAnnouncementRequest request = new CreateAnnouncementRequest();
        request.setTitle("T");
        request.setBody("B");
        courseAnnouncementService.create(17, 10, request);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(announcementNotificationService).afterCommit(captor.capture());
        assertNotNull(captor.getValue());
    }
}
