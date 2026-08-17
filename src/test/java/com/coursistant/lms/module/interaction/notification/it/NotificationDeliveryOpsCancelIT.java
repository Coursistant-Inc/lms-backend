package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationDeliveryOpsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;

class NotificationDeliveryOpsCancelIT extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private NotificationDeliveryOpsService opsService;

    @MockitoSpyBean
    private NotificationDigestEmailMapper digestEmailMapper;

    @AfterEach
    void resetSpies() {
        Mockito.reset(digestEmailMapper);
    }

    @Test
    void digestCancelThrows_rollsBackDeliveryCancel() {
        int userId = insertUser("ops-cancel@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        long deliveryId = insertDelivery(uuid(), userId, "ASSIGNMENT_SUBMISSION_RECEIVED",
                "ASSIGNMENT_SUBMISSION", 3, "receipt-" + uuid(), "IMMEDIATE_EMAIL", "PENDING",
                courseId, null);
        jdbcTemplate.update("""
                        INSERT INTO notification_digest_email (
                          tenant_id, recipient_user_id, digest_date, status, item_count, attempt_count,
                          next_attempt_at, unknown_outcome_count, created_at, updated_at
                        ) VALUES (1, ?, ?, 'PENDING', 1, 0, UTC_TIMESTAMP(3), 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                        """,
                userId, DIGEST_DATE);
        doThrow(new IllegalStateException("injected-digest-cancel-failure"))
                .when(digestEmailMapper).cancelPendingForRecipient(anyInt(), any());

        assertThrows(IllegalStateException.class, () -> opsService.cancelPendingEmailsFor(userId));

        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_delivery WHERE id = ?", String.class, deliveryId));
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE recipient_user_id = ?", String.class, userId));
    }
}
