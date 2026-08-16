package com.coursistant.lms.module.interaction.notification.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationOrphanRecoveryIT {

    @BeforeAll
    static void start() {
        NotificationPhase1Mysql.ensureStarted();
    }

    @Test
    void expiredProcessingLease_isSelectableForClaim() {
        JdbcTemplate jdbc = NotificationPhase1Mysql.jdbc();
        String eventId = NotificationPhase1Mysql.uuid();
        long id = NotificationPhase1Mysql.insertDelivery(eventId, 7, "ASSIGNMENT_GRADE_RELEASED",
                "ASSIGNMENT", 9, "orphan-" + eventId, "IMMEDIATE_EMAIL", "PROCESSING");
        jdbc.update("""
                UPDATE notification_delivery
                SET lease_until = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 MINUTE),
                    next_attempt_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 MINUTE),
                    claim_token = 'old-token',
                    send_attempted_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 4 MINUTE)
                WHERE id = ?
                """, id);
        Long claimed = jdbc.queryForObject("""
                SELECT id FROM notification_delivery
                WHERE channel = 'IMMEDIATE_EMAIL'
                  AND status IN ('PENDING', 'FAILED_RETRYABLE', 'PROCESSING')
                  AND next_attempt_at <= UTC_TIMESTAMP(3)
                  AND (lease_until IS NULL OR lease_until < UTC_TIMESTAMP(3))
                  AND id = ?
                """, Long.class, id);
        assertEquals(id, claimed);

        int promoted = jdbc.update("""
                UPDATE notification_delivery
                SET unknown_outcome_count = 1, send_attempted_at = NULL
                WHERE id = ? AND send_attempted_at IS NOT NULL AND unknown_outcome_count = 0
                """, id);
        assertEquals(1, promoted);
        Integer unknown = jdbc.queryForObject(
                "SELECT unknown_outcome_count FROM notification_delivery WHERE id = ?", Integer.class, id);
        assertEquals(1, unknown);
        assertTrue(jdbc.queryForObject(
                "SELECT send_attempted_at IS NULL FROM notification_delivery WHERE id = ?", Boolean.class, id));
    }
}
