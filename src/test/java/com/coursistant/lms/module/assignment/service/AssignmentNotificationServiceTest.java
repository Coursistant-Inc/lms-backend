package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentNotificationServiceTest {

    @Mock private CourseMapper courseMapper;
    @Mock private NotificationMessageFactory notificationMessageFactory;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @Mock private NotificationRecipientResolver notificationRecipientResolver;
    @InjectMocks private AssignmentNotificationService service;

    @Test
    void recordAssignmentPublished_usesPublicationVersionAndExplicitSnapshot() {
        Assignment assignment = new Assignment();
        assignment.setId(9);
        assignment.setCourseId(2);
        assignment.setTitle("HW");
        assignment.setPublicationVersion(3);
        Course course = new Course();
        course.setId(2);
        course.setTenantId(1);
        course.setCourseCode("CS101");
        course.setTitle("Intro");
        when(courseMapper.selectById(2)).thenReturn(course);
        when(notificationTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 17, 1, 0));
        when(notificationMessageFactory.assignmentPublished("HW")).thenReturn("New assignment published: HW");
        when(notificationRecipientResolver.resolveForType(NotificationType.ASSIGNMENT_PUBLISHED, 2, 4))
                .thenReturn(List.of(10, 11));
        when(notificationPublisher.publishInTransaction(any())).thenReturn(1L);

        service.recordAssignmentPublished(assignment, 4);

        ArgumentCaptor<NotificationDispatchPayload> captor =
                ArgumentCaptor.forClass(NotificationDispatchPayload.class);
        verify(notificationPublisher).publishInTransaction(captor.capture());
        NotificationDispatchPayload payload = captor.getValue();
        assertEquals(NotificationType.ASSIGNMENT_PUBLISHED, payload.getNotificationType());
        assertEquals("assignment:9:publication:3", payload.getEventKey());
        assertEquals(RecipientMode.EXPLICIT, payload.getRecipientMode());
        assertEquals(List.of(10, 11), payload.getRecipientIds());
        assertEquals(4, payload.getActorUserId());
    }
}
