package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * Pins digest terminal transitions: parent token update and item cascade stay in one transaction.
 */
class DigestTerminalCascadeIT extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private NotificationClaimService claimService;

    @Autowired
    private NotificationProperties notificationProperties;

    @Autowired
    private NotificationTimeSupport notificationTimeSupport;

    @Autowired
    private DailyDigestService dailyDigestService;

    @MockitoSpyBean
    private NotificationDigestEmailMapper digestEmailMapper;

    @AfterEach
    void resetSpies() {
        Mockito.reset(digestEmailMapper);
    }

    @Test
    void orphanMaxAttempts_marksItemsPermanentWithParent() {
        long digestId = seedDigestWithItem(5, null, 0, true, "cascade-orphan-" + uuid() + "@example.com");
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
        long digestId = seedDigestWithItem(1, sentAt, 1, true, "cascade-unknown-" + uuid() + "@example.com");
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

    @Test
    void sendOne_preferenceOff_cascadesSkippedPreference() {
        long digestId = seedDigestWithItem(0, null, 0, false, "cascade-pref-" + uuid() + "@example.com");
        dailyDigestService.sendOne(digestId);

        assertParentAndItems(digestId, "SKIPPED_PREFERENCE", "SKIPPED_PREFERENCE");
        assertEquals("PERMANENT_PREFERENCE", jdbcTemplate.queryForObject(
                "SELECT failure_category FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
    }

    @Test
    void sendOne_noUsableEmail_cascadesPermanent() {
        long digestId = seedDigestWithItem(0, null, 0, true, "no-at-sign-" + uuid());
        dailyDigestService.sendOne(digestId);

        assertParentAndItems(digestId, "FAILED_PERMANENT", "FAILED_PERMANENT");
        assertEquals("PERMANENT_NO_EMAIL", jdbcTemplate.queryForObject(
                "SELECT failure_category FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
    }

    @Test
    void sendOne_providerSent_cascadesSent() {
        long digestId = seedDigestWithItem(0, null, 0, true, "cascade-sent-" + uuid() + "@example.com");
        fakeNotificationEmailSender.succeed("smtp-1");
        dailyDigestService.sendOne(digestId);

        assertParentAndItems(digestId, "SENT", "SENT");
        assertEquals("smtp-1", jdbcTemplate.queryForObject(
                "SELECT provider_message_id FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertEquals(1, fakeNotificationEmailSender.messages().size());
    }

    @Test
    void sendOne_dryRun_cascadesDryRun() {
        long digestId = seedDigestWithItem(0, null, 0, true, "cascade-dry-" + uuid() + "@example.com");
        dailyDigestService.sendOne(digestId);

        assertParentAndItems(digestId, "DRY_RUN", "DRY_RUN");
        assertEquals(1, fakeNotificationEmailSender.messages().size());
    }

    @Test
    void sendOne_providerPermanent_cascadesFailedPermanent() {
        long digestId = seedDigestWithItem(0, null, 0, true, "cascade-perm-" + uuid() + "@example.com");
        fakeNotificationEmailSender.failPermanent(FailureCategory.PERMANENT_INVALID_EMAIL, "bad-addr");
        dailyDigestService.sendOne(digestId);

        assertParentAndItems(digestId, "FAILED_PERMANENT", "FAILED_PERMANENT");
        assertEquals("PERMANENT_INVALID_EMAIL", jdbcTemplate.queryForObject(
                "SELECT failure_category FROM notification_digest_email WHERE id = ?", String.class, digestId));
    }

    @Test
    void sendOne_staleParentUpdate_doesNotTouchItems() {
        long digestId = seedDigestWithItem(0, null, 0, false, "cascade-stale-" + uuid() + "@example.com");
        doReturn(0).when(digestEmailMapper).markSkipped(anyLong(), anyString(), anyString(), anyString(), any());

        dailyDigestService.sendOne(digestId);

        assertEquals("PROCESSING", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertEquals(1, count(
                "SELECT COUNT(*) FROM notification_delivery WHERE digest_email_id = ? AND status = 'PROCESSING'",
                digestId));
        assertTrue(fakeNotificationEmailSender.messages().isEmpty());
    }

    private void assertParentAndItems(long digestId, String parentStatus, String itemStatus) {
        assertEquals(parentStatus, jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, digestId));
        assertEquals(0, count(
                "SELECT COUNT(*) FROM notification_delivery WHERE digest_email_id = ? AND status = 'PROCESSING'",
                digestId));
        assertEquals(1, count(
                "SELECT COUNT(*) FROM notification_delivery WHERE digest_email_id = ? AND status = ?",
                digestId, itemStatus));
    }

    private long seedDigestWithItem(int attemptCount, LocalDateTime sendAttemptedAt, int unknownCount,
                                    boolean emailNotifications, String email) {
        int userId = insertUser(email, emailNotifications, "ACTIVE");
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
