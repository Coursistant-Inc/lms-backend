package com.coursistant.lms.module.interaction.notification.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationChannelPartialFailureIT {

    @BeforeAll
    static void start() {
        NotificationPhase1Mysql.ensureStarted();
    }

    @Test
    void retryFillsMissingChannel_withoutRewritingSnapshotOrDuplicateInApp() {
        JdbcTemplate jdbc = NotificationPhase1Mysql.jdbc();
        String eventId = NotificationPhase1Mysql.uuid();
        String eventKey = "partial-" + eventId;
        long outboxId = NotificationPhase1Mysql.insertOutbox(eventId, "ASSIGNMENT_GRADE_RELEASED",
                "ASSIGNMENT", 9, eventKey, "FAILED_RETRYABLE");
        jdbc.update("INSERT INTO notification_event_recipient (outbox_id, recipient_user_id) VALUES (?, 4), (?, 5)",
                outboxId, outboxId);
        NotificationPhase1Mysql.insertDelivery(eventId, 4, "ASSIGNMENT_GRADE_RELEASED",
                "ASSIGNMENT", 9, eventKey, "IN_APP", "SENT");
        NotificationPhase1Mysql.insertDelivery(eventId, 5, "ASSIGNMENT_GRADE_RELEASED",
                "ASSIGNMENT", 9, eventKey, "IN_APP", "SENT");

        jdbc.update("""
                INSERT INTO notification_delivery (
                  event_id, tenant_id, recipient_user_id, course_id, notification_type, subject_type,
                  subject_id, event_key, channel, status, message, deep_link, occurred_at,
                  attempt_count, next_attempt_at, unknown_outcome_count, created_at, updated_at
                ) VALUES (?, 1, 4, 2, 'ASSIGNMENT_GRADE_RELEASED', 'ASSIGNMENT', 9, ?, 'IN_APP', 'SENT',
                  'msg', '/x', UTC_TIMESTAMP(3), 0, UTC_TIMESTAMP(3), 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                ON DUPLICATE KEY UPDATE id = id
                """, eventId, eventKey);
        NotificationPhase1Mysql.insertDelivery(eventId, 4, "ASSIGNMENT_GRADE_RELEASED",
                "ASSIGNMENT", 9, eventKey, "IMMEDIATE_EMAIL", "PENDING");
        NotificationPhase1Mysql.insertDelivery(eventId, 5, "ASSIGNMENT_GRADE_RELEASED",
                "ASSIGNMENT", 9, eventKey, "IMMEDIATE_EMAIL", "PENDING");

        jdbc.update("""
                INSERT INTO notification_event_outbox (
                  event_id, tenant_id, course_id, notification_type, subject_type, subject_id, event_key,
                  message, deep_link, occurred_at, recipient_mode, status, attempt_count, next_attempt_at,
                  created_at, updated_at
                ) VALUES (?, 1, 2, 'ASSIGNMENT_GRADE_RELEASED', 'ASSIGNMENT', 9, ?, 'msg', '/x',
                  UTC_TIMESTAMP(3), 'EXPLICIT', 'PENDING', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                ON DUPLICATE KEY UPDATE id = id
                """, NotificationPhase1Mysql.uuid(), eventKey);

        Integer inApp = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_delivery WHERE event_id = ? AND channel = 'IN_APP'",
                Integer.class, eventId);
        Integer email = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_delivery WHERE event_id = ? AND channel = 'IMMEDIATE_EMAIL'",
                Integer.class, eventId);
        Integer recipients = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_event_recipient WHERE outbox_id = ?",
                Integer.class, outboxId);
        assertEquals(2, inApp);
        assertEquals(2, email);
        assertEquals(2, recipients);

        jdbc.update("UPDATE notification_event_outbox SET status = 'DONE' WHERE id = ?", outboxId);
        assertEquals("DONE", jdbc.queryForObject(
                "SELECT status FROM notification_event_outbox WHERE id = ?", String.class, outboxId));
    }
}
