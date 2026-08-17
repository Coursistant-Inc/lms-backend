package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

/**
 * Pins DailyDigestService.run → collectOne self-invocation: @Transactional does not apply,
 * so attachDigestItems can commit while freezeCollected never runs.
 */
class DigestPhaseATransactionIT extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private DailyDigestService dailyDigestService;

    @MockitoSpyBean
    private NotificationDigestEmailMapper digestEmailMapper;

    @Test
    void run_attachThenThrow_rollsBackDigestEmailAndItemsTogether() {
        int userId = insertUser("digest-a@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        insertDelivery(uuid(), userId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9, "ann-" + uuid(),
                "DAILY_DIGEST", "PENDING", courseId, DIGEST_DATE);

        doThrow(new IllegalStateException("injected-before-freeze"))
                .when(digestEmailMapper).freezeCollected(anyLong(), anyInt(), any());

        dailyDigestService.run(DIGEST_DATE, 1);

        int split = count("""
                SELECT COUNT(*) FROM notification_digest_email e
                WHERE e.status = 'COLLECTING'
                  AND EXISTS (
                    SELECT 1 FROM notification_delivery d
                    WHERE d.digest_email_id = e.id AND d.status = 'PROCESSING'
                  )
                """);
        int pendingItems = count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE channel = 'DAILY_DIGEST' AND status = 'PENDING' AND recipient_user_id = ?
                """, userId);

        assertEquals(0, split, "digest email and items must roll back together");
        assertEquals(1, pendingItems, "unfrozen digest items must remain PENDING");
    }
}
