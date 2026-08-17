package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins digest terminal transitions that only update the parent row, leaving items PROCESSING.
 */
class DigestTerminalCascadeIT extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private NotificationClaimService claimService;

    @Autowired
    private NotificationProperties notificationProperties;

    @Autowired
    private NotificationTimeSupport notificationTimeSupport;

    @Test
    void orphanMaxAttempts_marksItemsPermanentWithParent() {
        long digestId = seedDigestWithItem(5, null, 0);
        LocalDateTime now = notificationTimeSupport.nowUtc();
        claimService.claimDigestEmail(digestId, now, now.plusMinutes(2),
                notificationProperties.getDigest().getMaxAttempts());

        String parent = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, digestId);
        int processingItems = count(
                "SELECT COUNT(*) FROM notification_delivery WHERE digest_email_id = ? AND status = 'PROCESSING'",
                digestId);
        assertEquals("FAILED_PERMANENT", parent);
        assertEquals(0, processingItems, "terminal parent must cascade items off PROCESSING");
    }

    @Test
    void secondUnknownOutcome_marksItemsPermanentWithParent() {
        LocalDateTime sentAt = notificationTimeSupport.nowUtc().minusMinutes(5);
        long digestId = seedDigestWithItem(1, sentAt, 1);
        LocalDateTime now = notificationTimeSupport.nowUtc();
        claimService.claimDigestEmail(digestId, now, now.plusMinutes(2),
                notificationProperties.getDigest().getMaxAttempts());

        String parent = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, digestId);
        int processingItems = count(
                "SELECT COUNT(*) FROM notification_delivery WHERE digest_email_id = ? AND status = 'PROCESSING'",
                digestId);
        assertEquals("FAILED_PERMANENT", parent);
        assertEquals(0, processingItems, "UNKNOWN_OUTCOME parent must cascade items off PROCESSING");
        assertEquals("UNKNOWN_OUTCOME", jdbcTemplate.queryForObject(
                "SELECT failure_category FROM notification_digest_email WHERE id = ?", String.class, digestId));
    }

    private long seedDigestWithItem(int attemptCount, LocalDateTime sendAttemptedAt, int unknownCount) {
        int userId = insertUser("cascade-" + uuid() + "@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        jdbcTemplate.update("""
                        INSERT INTO notification_digest_email (
                          tenant_id, recipient_user_id, digest_date, status, item_count, attempt_count,
                          next_attempt_at, unknown_outcome_count, send_attempted_at, created_at, updated_at
                        ) VALUES (1, ?, ?, 'PENDING', 1, ?, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE), ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                        """,
                userId, DIGEST_DATE, attemptCount, unknownCount, sendAttemptedAt);
        Long digestId = jdbcTemplate.queryForObject(
                "SELECT id FROM notification_digest_email WHERE recipient_user_id = ?", Long.class, userId);
        long deliveryId = insertDelivery(uuid(), userId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9,
                "ann-" + uuid(), "DAILY_DIGEST", "PROCESSING", courseId, DIGEST_DATE);
        jdbcTemplate.update("UPDATE notification_delivery SET digest_email_id = ? WHERE id = ?", digestId, deliveryId);
        return digestId == null ? -1L : digestId;
    }
}
