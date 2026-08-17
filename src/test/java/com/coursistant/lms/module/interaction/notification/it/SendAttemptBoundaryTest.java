package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.email.ImmediateEmailDeliveryWorker;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * Pins send-attempt marker order: no usable email must not stamp send_attempted_at,
 * and a 0-row marker update must not call the email provider.
 */
class SendAttemptBoundaryTest extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private DailyDigestService dailyDigestService;

    @Autowired
    private ImmediateEmailDeliveryWorker immediateEmailDeliveryWorker;

    @MockitoSpyBean
    private NotificationDigestEmailMapper digestEmailMapper;

    @MockitoSpyBean
    private NotificationDeliveryMapper deliveryMapper;

    @AfterEach
    void resetSpies() {
        Mockito.reset(digestEmailMapper, deliveryMapper);
    }

    @Test
    void digestSendOne_noUsableEmail_doesNotStampSendAttemptedAt() {
        long digestId = seedFrozenDigest("no-at-sign");
        dailyDigestService.sendOne(digestId);

        Boolean stamped = jdbcTemplate.queryForObject(
                "SELECT send_attempted_at IS NOT NULL FROM notification_digest_email WHERE id = ?",
                Boolean.class, digestId);
        assertEquals(Boolean.FALSE, stamped, "no usable email must not write send_attempted_at");
        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
    }

    @Test
    void immediateProcessOne_noUsableEmail_doesNotStampSendAttemptedAt() {
        int userId = insertUser("imm-invalid", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        long deliveryId = insertDelivery(uuid(), userId, "ASSIGNMENT_SUBMISSION_RECEIVED",
                "ASSIGNMENT_SUBMISSION", 3, "receipt-" + uuid(), "IMMEDIATE_EMAIL", "PENDING",
                courseId, null);
        immediateEmailDeliveryWorker.processOne(deliveryId);

        Boolean stamped = jdbcTemplate.queryForObject(
                "SELECT send_attempted_at IS NOT NULL FROM notification_delivery WHERE id = ?",
                Boolean.class, deliveryId);
        assertEquals(Boolean.FALSE, stamped);
        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
    }

    @Test
    void digestSendOne_staleMarker_doesNotCallProvider() {
        long digestId = seedFrozenDigest("stale-marker@example.com");
        doReturn(0).when(digestEmailMapper).markSendAttempted(anyLong(), anyString(), any());

        dailyDigestService.sendOne(digestId);

        assertTrue(fakeNotificationEmailSender.messages().isEmpty(),
                "0-row marker update must skip the email provider");
    }

    @Test
    void immediateProcessOne_staleMarker_doesNotCallProvider() {
        int userId = insertUser("stale-imm@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        long deliveryId = insertDelivery(uuid(), userId, "ASSIGNMENT_SUBMISSION_RECEIVED",
                "ASSIGNMENT_SUBMISSION", 3, "receipt-" + uuid(), "IMMEDIATE_EMAIL", "PENDING",
                courseId, null);
        doReturn(0).when(deliveryMapper).markSendAttempted(anyLong(), anyString(), any());

        immediateEmailDeliveryWorker.processOne(deliveryId);

        assertTrue(fakeNotificationEmailSender.messages().isEmpty(),
                "0-row marker update must skip the email provider");
    }

    private long seedFrozenDigest(String email) {
        int userId = insertUser(email, true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        jdbcTemplate.update("""
                        INSERT INTO notification_digest_email (
                          tenant_id, recipient_user_id, digest_date, status, item_count, attempt_count,
                          next_attempt_at, unknown_outcome_count, created_at, updated_at
                        ) VALUES (1, ?, ?, 'PENDING', 1, 0, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE), 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                        """,
                userId, DIGEST_DATE);
        Long digestId = jdbcTemplate.queryForObject(
                "SELECT id FROM notification_digest_email WHERE recipient_user_id = ?", Long.class, userId);
        long deliveryId = insertDelivery(uuid(), userId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9,
                "ann-" + uuid(), "DAILY_DIGEST", "PROCESSING", courseId, DIGEST_DATE);
        jdbcTemplate.update(
                "UPDATE notification_delivery SET digest_email_id = ? WHERE id = ?", digestId, deliveryId);
        return digestId == null ? -1L : digestId;
    }
}
