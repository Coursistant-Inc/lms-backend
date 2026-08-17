package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizNotificationServiceTest {

    @Mock private CourseMapper courseMapper;
    @Mock private NotificationMessageFactory notificationMessageFactory;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @Mock private NotificationRecipientResolver notificationRecipientResolver;
    @Mock private TenantTimezoneService tenantTimezoneService;
    @InjectMocks private QuizNotificationService service;

    @Test
    void publishAndScheduleAndTimeLimit_useDistinctKeys() {
        Quiz quiz = new Quiz();
        quiz.setId(5);
        quiz.setCourseId(2);
        quiz.setTitle("Midterm");
        quiz.setPublicationVersion(2);
        quiz.setVersion(8);
        quiz.setOpensAt(LocalDateTime.of(2026, 8, 17, 10, 0));
        quiz.setClosesAt(LocalDateTime.of(2026, 8, 17, 12, 0));
        Course course = new Course();
        course.setId(2);
        course.setTenantId(1);
        course.setCourseCode("CS101");
        course.setTitle("Intro");
        when(courseMapper.selectById(2)).thenReturn(course);
        when(notificationTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 17, 1, 0));
        when(notificationRecipientResolver.resolveForType(any(), any(), any())).thenReturn(List.of(10));
        when(notificationPublisher.publishInTransaction(any())).thenReturn(1L);
        when(notificationMessageFactory.quizPublished("Midterm")).thenReturn("New quiz published: Midterm");
        when(tenantTimezoneService.requireZoneForCourse(2)).thenReturn(ZoneOffset.UTC);
        when(notificationMessageFactory.formatWindow(any(), any(), any())).thenReturn("window");
        when(notificationMessageFactory.quizScheduleChanged("Midterm", "window")).thenReturn("sched");
        when(notificationMessageFactory.quizTimeLimitChanged("Midterm")).thenReturn("limit");

        service.recordQuizPublished(quiz, 4);
        service.recordScheduleChanged(quiz, 4);
        service.recordTimeLimitChanged(quiz, 4);

        ArgumentCaptor<NotificationDispatchPayload> captor =
                ArgumentCaptor.forClass(NotificationDispatchPayload.class);
        verify(notificationPublisher, org.mockito.Mockito.times(3)).publishInTransaction(captor.capture());
        assertEquals("quiz:5:publication:2", captor.getAllValues().get(0).getEventKey());
        assertEquals(NotificationType.QUIZ_PUBLISHED, captor.getAllValues().get(0).getNotificationType());
        assertEquals("quiz:5:schedule:8", captor.getAllValues().get(1).getEventKey());
        assertEquals("quiz:5:time-limit:8", captor.getAllValues().get(2).getEventKey());
    }
}
