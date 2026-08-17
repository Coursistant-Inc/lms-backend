package com.coursistant.lms.module.interaction.notification.claim;

import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationClaimServiceTest {

    @Mock private NotificationDeliveryMapper deliveryMapper;
    @Mock private NotificationDigestEmailMapper digestEmailMapper;
    @Mock private NotificationEventOutboxMapper outboxMapper;
    @InjectMocks private NotificationClaimService claimService;

    private final LocalDateTime now = LocalDateTime.of(2026, 8, 16, 1, 0);

    @Test
    void claimConflict_returnsEmpty() {
        when(deliveryMapper.claim(eq(9L), anyString(), any(), eq(now))).thenReturn(0);
        assertTrue(claimService.claimDelivery(9L, now, now.plusMinutes(2), 5).isEmpty());
        verify(deliveryMapper, never()).selectById(9L);
    }

    @Test
    void unknownOnce_promotesAndReturnsRow() {
        when(deliveryMapper.claim(eq(9L), anyString(), any(), eq(now))).thenReturn(1);
        NotificationDelivery row = new NotificationDelivery();
        row.setId(9L);
        row.setAttemptCount(1);
        row.setSendAttemptedAt(now.minusMinutes(5));
        row.setUnknownOutcomeCount(0);
        row.setStatus("PROCESSING");
        when(deliveryMapper.selectById(9L)).thenReturn(row);
        when(deliveryMapper.promoteUnknownOnce(eq(9L), anyString(), eq(now))).thenReturn(1);

        Optional<NotificationClaimService.Claimed<NotificationDelivery>> claimed =
                claimService.claimDelivery(9L, now, now.plusMinutes(2), 5);
        assertTrue(claimed.isPresent());
        assertEquals(1, claimed.get().row().getUnknownOutcomeCount());
        verify(deliveryMapper, never()).markPermanent(any(), any(), any(), any(), any());
    }

    @Test
    void unknownAlreadyCounted_marksPermanent() {
        when(deliveryMapper.claim(eq(9L), anyString(), any(), eq(now))).thenReturn(1);
        NotificationDelivery row = new NotificationDelivery();
        row.setId(9L);
        row.setAttemptCount(2);
        row.setSendAttemptedAt(now.minusMinutes(5));
        row.setUnknownOutcomeCount(1);
        when(deliveryMapper.selectById(9L)).thenReturn(row);

        assertTrue(claimService.claimDelivery(9L, now, now.plusMinutes(2), 5).isEmpty());
        verify(deliveryMapper).markPermanent(eq(9L), anyString(),
                eq(FailureCategory.UNKNOWN_OUTCOME.name()), any(), eq(now));
    }

    @Test
    void promoteZeroRows_returnsEmpty() {
        when(deliveryMapper.claim(eq(9L), anyString(), any(), eq(now))).thenReturn(1);
        NotificationDelivery row = new NotificationDelivery();
        row.setId(9L);
        row.setAttemptCount(1);
        row.setSendAttemptedAt(now.minusMinutes(1));
        row.setUnknownOutcomeCount(0);
        when(deliveryMapper.selectById(9L)).thenReturn(row);
        when(deliveryMapper.promoteUnknownOnce(eq(9L), anyString(), eq(now))).thenReturn(0);

        assertTrue(claimService.claimDelivery(9L, now, now.plusMinutes(2), 5).isEmpty());
    }

    @Test
    void attemptOverflow_marksOrphanPermanent() {
        when(deliveryMapper.claim(eq(9L), anyString(), any(), eq(now))).thenReturn(1);
        NotificationDelivery row = new NotificationDelivery();
        row.setId(9L);
        row.setAttemptCount(6);
        when(deliveryMapper.selectById(9L)).thenReturn(row);

        assertTrue(claimService.claimDelivery(9L, now, now.plusMinutes(2), 5).isEmpty());
        verify(deliveryMapper).markPermanent(eq(9L), anyString(),
                eq(FailureCategory.ORPHAN_MAX_ATTEMPTS.name()), any(), eq(now));
    }
}
