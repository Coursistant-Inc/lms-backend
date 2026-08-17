package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins Connector/J default found-rows on ON DUPLICATE KEY UPDATE id=id: a second publish
 * with extra recipients must not expand the frozen EXPLICIT snapshot.
 */
class NotificationOutboxDuplicateMysqlIT extends NotificationPhase1SpringITBase {

    @Autowired
    private NotificationPublisher publisher;

    @Test
    void secondPublish_sameBusinessKey_keepsOriginalSnapshot() {
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        int a = insertUser("snap-a@example.com", true, "ACTIVE");
        int b = insertUser("snap-b@example.com", true, "ACTIVE");
        int c = insertUser("snap-c@example.com", true, "ACTIVE");

        NotificationDispatchPayload first = event(courseId, List.of(a, b));
        NotificationDispatchPayload second = event(courseId, List.of(a, b, c));
        second.setEventId(uuid());

        transactionTemplate.executeWithoutResult(status -> publisher.publishInTransaction(first));
        assertDoesNotThrow(() ->
                transactionTemplate.executeWithoutResult(status -> publisher.publishInTransaction(second)));

        int outboxRows = count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE tenant_id = 1 AND notification_type = 'ASSIGNMENT_GRADE_RELEASED'
                  AND subject_type = 'ASSIGNMENT' AND subject_id = 9 AND event_key = 'release:7'
                """);
        List<Integer> snapshot = jdbcTemplate.queryForList("""
                SELECT r.recipient_user_id
                FROM notification_event_recipient r
                JOIN notification_event_outbox o ON o.id = r.outbox_id
                WHERE o.event_key = 'release:7'
                ORDER BY r.recipient_user_id
                """, Integer.class);

        assertEquals(1, outboxRows);
        assertEquals(List.of(a, b), snapshot);
    }

    private NotificationDispatchPayload event(int courseId, List<Integer> recipients) {
        NotificationDispatchPayload event = new NotificationDispatchPayload();
        event.setNotificationType(NotificationType.ASSIGNMENT_GRADE_RELEASED);
        event.setTenantId(1);
        event.setCourseId(courseId);
        event.setSubjectType(SubjectType.ASSIGNMENT);
        event.setSubjectId(9);
        event.setEventKey("release:7");
        event.setMessage("released");
        event.setDeepLink("/x");
        event.setRecipientMode(RecipientMode.EXPLICIT);
        event.setRecipientIds(recipients);
        return event;
    }
}
