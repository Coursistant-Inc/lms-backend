package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.ExplicitRecipientValidator;
import com.coursistant.lms.module.interaction.notification.service.NotificationFanoutService;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventRelayWorkerTest {

    @Mock private NotificationEventOutboxMapper outboxMapper;
    @Mock private NotificationEventRecipientMapper recipientMapper;
    @Mock private NotificationFanoutService notificationFanoutService;
    @Mock private NotificationRecipientResolver notificationRecipientResolver;
    @Mock private ExplicitRecipientValidator explicitRecipientValidator;
    @Mock private NotificationClaimService claimService;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @Mock private PlatformTransactionManager transactionManager;
    @InjectMocks private NotificationEventRelayWorker worker;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "notificationJson",
                new NotificationJson(new ObjectMapper()));
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "notificationProperties",
                new NotificationProperties());
        when(notificationTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 16, 1, 0));
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void allPersisted_marksDone() {
        when(claimService.claimOutbox(eq(5L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(outbox(), "tok")));
        when(outboxMapper.lockClaimed(eq(5L), eq("tok"), any())).thenReturn(outbox());
        when(recipientMapper.selectRecipientIds(eq(5L), anyInt(), anyInt())).thenReturn(List.of(8));
        when(explicitRecipientValidator.validate(eq(1), anyList())).thenReturn(List.of(8));
        when(outboxMapper.markDone(eq(5L), eq("tok"), any())).thenReturn(1);

        worker.processOne(5L);

        verify(notificationFanoutService).persist(any(), eq(List.of(8)));
        verify(outboxMapper).markDone(eq(5L), eq("tok"), any());
        verify(outboxMapper, never()).markRetryable(any(), any(), any(), any(), any());
        verifyNoInteractions(notificationRecipientResolver);
        verify(explicitRecipientValidator, never()).shouldExcludeActor(any(), any(), any());
    }

    @Test
    void fanoutThrows_marksRetryable() {
        when(claimService.claimOutbox(eq(5L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(outbox(), "tok")));
        when(outboxMapper.lockClaimed(eq(5L), eq("tok"), any())).thenReturn(outbox());
        when(recipientMapper.selectRecipientIds(eq(5L), anyInt(), anyInt())).thenReturn(List.of(8));
        when(explicitRecipientValidator.validate(eq(1), anyList())).thenReturn(List.of(8));
        doThrow(new RuntimeException("fail")).when(notificationFanoutService).persist(any(), anyList());
        when(outboxMapper.markRetryable(eq(5L), eq("tok"), any(), anyString(), any())).thenReturn(1);

        worker.processOne(5L);

        verify(outboxMapper).markRetryable(eq(5L), eq("tok"), any(), anyString(), any());
        verify(outboxMapper, never()).markDone(any(), any(), any());
    }

    @Test
    void markRetryableZero_doesNotThrow() {
        when(claimService.claimOutbox(eq(5L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(outbox(), "tok")));
        when(outboxMapper.lockClaimed(eq(5L), eq("tok"), any())).thenReturn(outbox());
        when(recipientMapper.selectRecipientIds(eq(5L), anyInt(), anyInt())).thenReturn(List.of(8));
        when(explicitRecipientValidator.validate(eq(1), anyList())).thenReturn(List.of(8));
        doThrow(new RuntimeException("fail")).when(notificationFanoutService).persist(any(), anyList());
        when(outboxMapper.markRetryable(eq(5L), eq("tok"), any(), anyString(), any())).thenReturn(0);

        worker.processOne(5L);

        verify(outboxMapper).markRetryable(eq(5L), eq("tok"), any(), anyString(), any());
    }

    @Test
    void recipientResolutionThrows_marksRetryable() {
        when(claimService.claimOutbox(eq(5L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(outbox(), "tok")));
        when(outboxMapper.lockClaimed(eq(5L), eq("tok"), any())).thenReturn(outbox());
        when(recipientMapper.selectRecipientIds(eq(5L), anyInt(), anyInt())).thenReturn(List.of(8));
        when(explicitRecipientValidator.validate(eq(1), anyList())).thenThrow(new RuntimeException("db down"));
        when(outboxMapper.markRetryable(eq(5L), eq("tok"), any(), anyString(), any())).thenReturn(1);

        worker.processOne(5L);

        verify(outboxMapper).markRetryable(eq(5L), eq("tok"), any(), eq("db down"), any());
        verify(outboxMapper, never()).markDone(any(), any(), any());
        verifyNoInteractions(notificationFanoutService);
    }

    private NotificationEventOutbox outbox() {
        NotificationEventOutbox row = new NotificationEventOutbox();
        row.setId(5L);
        row.setEventId("e");
        row.setTenantId(1);
        row.setCourseId(2);
        row.setNotificationType(NotificationType.ASSIGNMENT_GRADE_RELEASED.name());
        row.setSubjectType(SubjectType.ASSIGNMENT.name());
        row.setSubjectId(9);
        row.setEventKey("release:1");
        row.setMessage("m");
        row.setDeepLink("/x");
        row.setRecipientMode(RecipientMode.EXPLICIT.name());
        row.setAttemptCount(1);
        row.setOccurredAt(LocalDateTime.of(2026, 8, 16, 1, 0));
        return row;
    }
}
