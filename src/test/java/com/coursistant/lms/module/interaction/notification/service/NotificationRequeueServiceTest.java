package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRequeueServiceTest {

    @Mock private NotificationDeliveryMapper deliveryMapper;
    @Mock private NotificationDigestEmailMapper digestEmailMapper;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @InjectMocks private NotificationDeliveryOpsService service;

    private final LocalDateTime now = LocalDateTime.of(2026, 8, 16, 1, 0);

    @BeforeEach
    void init() {
        org.mockito.Mockito.lenient().when(notificationTimeSupport.nowUtc()).thenReturn(now);
    }

    @Test
    void requeueDryRun_resetsDeliveriesAndWholeDigestEmails() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 16, 0, 0);
        when(deliveryMapper.requeueDryRunInRange(from, to, 1, "IMMEDIATE_EMAIL", now)).thenReturn(3);
        when(digestEmailMapper.selectDryRunOrPermanentIds(from, to, 1)).thenReturn(List.of(20L, 21L));
        when(digestEmailMapper.requeueDryRunInRange(from, to, 1, now)).thenReturn(2);

        int n = service.requeueDryRun(from, to, 1, "IMMEDIATE_EMAIL");

        assertTrue(n >= 5);
        verify(deliveryMapper).restoreItemsForDigestEmails(List.of(20L, 21L));
    }

    @Test
    void requeueSql_resetsNineClaimFields() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/interaction/NotificationDeliveryMapper.xml"));
        int start = xml.indexOf("<update id=\"requeueDelivery\">");
        int end = xml.indexOf("</update>", start);
        String sql = xml.substring(start, end);
        for (String field : List.of(
                "claim_token = NULL",
                "lease_until = NULL",
                "attempt_count = 0",
                "next_attempt_at = #{now}",
                "send_attempted_at = NULL",
                "unknown_outcome_count = 0",
                "provider_message_id = NULL",
                "sent_at = NULL",
                "failure_category = NULL")) {
            assertTrue(sql.contains(field), field);
        }

        String digest = Files.readString(Path.of(
                "src/main/resources/mapper/interaction/NotificationDigestEmailMapper.xml"));
        int dStart = digest.indexOf("<update id=\"requeueDryRunInRange\">");
        int dEnd = digest.indexOf("</update>", dStart);
        String digestSql = digest.substring(dStart, dEnd);
        assertTrue(digestSql.contains("status = 'PENDING'"));
        assertTrue(digestSql.contains("claim_token = NULL"));
        assertTrue(digestSql.contains("item_count") || digestSql.contains("status = 'PENDING'"));
    }

    @Test
    void retryDelivery_delegatesToMapper() {
        when(deliveryMapper.requeueDelivery(eq(9L), any())).thenReturn(1);
        service.retryDelivery(9L);
        verify(deliveryMapper).requeueDelivery(9L, now);
    }
}
