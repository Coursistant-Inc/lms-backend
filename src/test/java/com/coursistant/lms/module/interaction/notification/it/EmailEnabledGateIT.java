package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.email.ImmediateEmailDeliveryWorker;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailEnabledGateIT extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private DailyDigestService dailyDigestService;

    @Autowired
    private ImmediateEmailDeliveryWorker immediateEmailDeliveryWorker;

    @Autowired
    private NotificationProperties notificationProperties;

    @AfterEach
    void restoreEmailEnabled() {
        notificationProperties.getEmail().setEnabled(true);
    }

    @Test
    void digestRun_emailDisabled_collectsButDoesNotSend() {
        int userId = insertUser("gate-digest@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        insertDelivery(uuid(), userId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9, "ann-" + uuid(),
                "DAILY_DIGEST", "PENDING", courseId, DIGEST_DATE);
        notificationProperties.getEmail().setEnabled(false);

        dailyDigestService.run(DIGEST_DATE, 1);

        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
        int pendingEnvelopes = count("""
                SELECT COUNT(*) FROM notification_digest_email
                WHERE recipient_user_id = ? AND status = 'PENDING'
                """, userId);
        assertEquals(1, pendingEnvelopes, "Phase A may freeze envelopes; Phase B must not send");
    }

    @Test
    void digestSendOne_emailDisabled_doesNotClaim() {
        int userId = insertUser("gate-sendone@example.com", true, "ACTIVE");
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
        notificationProperties.getEmail().setEnabled(false);

        dailyDigestService.sendOne(digestId);

        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM notification_digest_email WHERE id = ?", Integer.class, digestId));
    }

    @Test
    void immediateProcessOne_emailDisabled_leavesPending() {
        int userId = insertUser("gate-imm@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        long deliveryId = insertDelivery(uuid(), userId, "ASSIGNMENT_SUBMISSION_RECEIVED",
                "ASSIGNMENT_SUBMISSION", 3, "receipt-" + uuid(), "IMMEDIATE_EMAIL", "PENDING",
                courseId, null);
        notificationProperties.getEmail().setEnabled(false);

        immediateEmailDeliveryWorker.processOne(deliveryId);

        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_delivery WHERE id = ?", String.class, deliveryId));
    }
}
