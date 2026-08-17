package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.email.ImmediateEmailDeliveryWorker;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins SMTP read/response timeout as UNKNOWN_OUTCOME: keep send_attempted_at and resend at most once.
 */
class SmtpUnknownOutcomeIT extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private ImmediateEmailDeliveryWorker immediateEmailDeliveryWorker;

    @Autowired
    private DailyDigestService dailyDigestService;

    @Test
    void immediate_twoReadTimeouts_becomeUnknownPermanent() {
        int userId = insertUser("smtp-unk-imm@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        long deliveryId = insertDelivery(uuid(), userId, "ASSIGNMENT_SUBMISSION_RECEIVED",
                "ASSIGNMENT_SUBMISSION", 3, "receipt-" + uuid(), "IMMEDIATE_EMAIL", "PENDING",
                courseId, null);
        fakeNotificationEmailSender.failUnknown(FailureCategory.UNKNOWN_OUTCOME, "smtp-read-timeout");

        for (int i = 0; i < 3; i++) {
            jdbcTemplate.update("""
                    UPDATE notification_delivery
                    SET next_attempt_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE),
                        lease_until = NULL
                    WHERE id = ?
                    """, deliveryId);
            immediateEmailDeliveryWorker.processOne(deliveryId);
        }

        assertEquals("FAILED_PERMANENT", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_delivery WHERE id = ?", String.class, deliveryId));
        assertEquals("UNKNOWN_OUTCOME", jdbcTemplate.queryForObject(
                "SELECT failure_category FROM notification_delivery WHERE id = ?", String.class, deliveryId));
        assertEquals(2, fakeNotificationEmailSender.messages().size());
    }

    @Test
    void digest_twoReadTimeouts_becomeUnknownPermanent() {
        int userId = insertUser("smtp-unk-digest@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        jdbcTemplate.update("""
                        INSERT INTO notification_digest_email (
                          tenant_id, recipient_user_id, digest_date, status, item_count, attempt_count,
                          next_attempt_at, unknown_outcome_count, created_at, updated_at
                        ) VALUES (1, ?, ?, 'PENDING', 1, 0, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE), 0,
                          UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                        """,
                userId, DIGEST_DATE);
        Long digestId = jdbcTemplate.queryForObject(
                "SELECT id FROM notification_digest_email WHERE recipient_user_id = ?", Long.class, userId);
        long deliveryId = insertDelivery(uuid(), userId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9,
                "ann-" + uuid(), "DAILY_DIGEST", "PROCESSING", courseId, DIGEST_DATE);
        jdbcTemplate.update(
                "UPDATE notification_delivery SET digest_email_id = ? WHERE id = ?", digestId, deliveryId);
        fakeNotificationEmailSender.failUnknown(FailureCategory.UNKNOWN_OUTCOME, "smtp-read-timeout");

        for (int i = 0; i < 3; i++) {
            jdbcTemplate.update("""
                    UPDATE notification_digest_email
                    SET next_attempt_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE),
                        lease_until = NULL
                    WHERE id = ?
                    """, digestId);
            dailyDigestService.sendOne(digestId);
        }

        assertEquals("FAILED_PERMANENT", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertEquals("UNKNOWN_OUTCOME", jdbcTemplate.queryForObject(
                "SELECT failure_category FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertEquals("FAILED_PERMANENT", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_delivery WHERE id = ?", String.class, deliveryId));
        assertEquals(2, fakeNotificationEmailSender.messages().size());
    }
}
