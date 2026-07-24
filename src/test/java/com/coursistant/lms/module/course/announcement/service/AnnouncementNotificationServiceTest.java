package com.coursistant.lms.module.course.announcement.service;

import com.coursistant.lms.module.course.announcement.entity.CourseAnnouncement;
import com.coursistant.lms.module.course.announcement.entity.UserNotification;
import com.coursistant.lms.module.course.announcement.repository.UserNotificationMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementNotificationServiceTest {

    @Mock
    private UserNotificationMapper userNotificationMapper;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @InjectMocks
    private AnnouncementNotificationService announcementNotificationService;

    @Test
    void notifyAnnouncementPosted_excludesAuthor_andInsertsOthers() {
        CourseAnnouncement announcement = new CourseAnnouncement();
        announcement.setId(9);
        announcement.setCourseId(17);
        announcement.setAuthorUserId(10);
        announcement.setTitle("News");

        Enrollment author = enrollment(10);
        Enrollment student = enrollment(385);
        Enrollment ta = enrollment(50);
        when(enrollmentMapper.selectActiveByCourseId(17)).thenReturn(List.of(author, student, ta));
        when(userNotificationMapper.insertIgnore(any(UserNotification.class))).thenReturn(1);

        announcementNotificationService.notifyAnnouncementPosted(announcement);

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(userNotificationMapper, times(2)).insertIgnore(captor.capture());
        List<UserNotification> inserted = captor.getAllValues();
        assertEquals(2, inserted.size());
        assertEquals(385, inserted.get(0).getRecipientUserId());
        assertEquals(50, inserted.get(1).getRecipientUserId());
        assertEquals(UserNotification.EVENT_ANNOUNCEMENT_POSTED, inserted.get(0).getEventType());
        assertEquals("/v2/courses/17/announcements/9", inserted.get(0).getDeepLink());
    }

    @Test
    void notifyAnnouncementPosted_mapperThrows_doesNotPropagate() {
        CourseAnnouncement announcement = new CourseAnnouncement();
        announcement.setId(9);
        announcement.setCourseId(17);
        announcement.setAuthorUserId(10);
        announcement.setTitle("News");

        when(enrollmentMapper.selectActiveByCourseId(17)).thenReturn(List.of(enrollment(385)));
        when(userNotificationMapper.insertIgnore(any(UserNotification.class)))
                .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> announcementNotificationService.notifyAnnouncementPosted(announcement));
    }

    private Enrollment enrollment(Integer userId) {
        Enrollment e = new Enrollment();
        e.setUserId(userId);
        e.setCourseId(17);
        e.setActive(true);
        return e;
    }
}
