package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins outbox.enabled=false: publisher must still write PENDING outbox and must not
 * afterCommit-dispatch into user_notification.
 */
@TestPropertySource(properties = "lms.notification.outbox.enabled=false")
class OutboxPausedModeIT extends NotificationPhase1SpringITBase {

    @Autowired
    private NotificationPublisher publisher;

    @Test
    void publish_whenOutboxDisabled_stillInsertsPendingOutboxAndDoesNotDirectWrite() {
        int instructorId = insertInstructor();
        int studentId = insertUser("paused@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);

        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setTenantId(1);
        payload.setCourseId(courseId);
        payload.setNotificationType(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED);
        payload.setSubjectType(SubjectType.ASSIGNMENT_SUBMISSION);
        payload.setSubjectId(21);
        payload.setEventKey("receipt:21");
        payload.setMessage("received");
        payload.setDeepLink("/s/21");
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setRecipientIds(List.of(studentId));
        payload.setActorUserId(studentId);

        transactionTemplate.executeWithoutResult(status -> publisher.publishInTransaction(payload));

        int outbox = count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE event_key = 'receipt:21' AND status = 'PENDING'
                """);
        int inApp = count("""
                SELECT COUNT(*) FROM user_notification
                WHERE event_key = 'receipt:21'
                """);
        assertEquals(1, outbox, "paused relay must still persist PENDING outbox");
        assertEquals(0, inApp, "paused relay must not afterCommit-dispatch in-app rows");
    }
}
