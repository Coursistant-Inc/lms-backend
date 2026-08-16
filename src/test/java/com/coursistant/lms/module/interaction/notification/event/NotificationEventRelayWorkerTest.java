package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.channel.ChannelPersistResult;
import com.coursistant.lms.module.interaction.notification.channel.DispatchOutcome;
import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelDispatcher;
import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelRouter;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.ExplicitRecipientValidator;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventRelayWorkerTest {

    @Mock private NotificationEventOutboxMapper outboxMapper;
    @Mock private NotificationEventRecipientMapper recipientMapper;
    @Mock private NotificationChannelDispatcher channelDispatcher;
    @Mock private NotificationRecipientResolver notificationRecipientResolver;
    @Mock private ExplicitRecipientValidator explicitRecipientValidator;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @InjectMocks private NotificationEventRelayWorker worker;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "channelRouter", new NotificationChannelRouter());
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "notificationJson", new NotificationJson(new ObjectMapper()));
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "notificationProperties", new NotificationProperties());
        when(notificationTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 16, 1, 0));
    }

    @Test
    void allPersisted_marksDone() {
        when(outboxMapper.claim(eq(5L), anyString(), any(), any())).thenReturn(1);
        when(outboxMapper.selectById(5L)).thenReturn(outbox());
        when(recipientMapper.selectRecipientIds(eq(5L), anyInt(), anyInt())).thenReturn(List.of(8));
        when(explicitRecipientValidator.validate(eq(1), anyList())).thenReturn(List.of(8));
        when(channelDispatcher.dispatch(any(), anyList())).thenReturn(new DispatchOutcome(List.of(
                new ChannelPersistResult(NotificationChannel.IN_APP, true, 1, null),
                new ChannelPersistResult(NotificationChannel.IMMEDIATE_EMAIL, true, 1, null)
        )));
        when(outboxMapper.markDone(eq(5L), anyString(), any())).thenReturn(1);

        worker.processOne(5L);

        verify(outboxMapper).markDone(eq(5L), anyString(), any());
        verify(outboxMapper, never()).markRetryable(any(), any(), any(), any(), any());
        verifyNoInteractions(notificationRecipientResolver);
    }

    @Test
    void partialPersist_marksRetryable() {
        when(outboxMapper.claim(eq(5L), anyString(), any(), any())).thenReturn(1);
        when(outboxMapper.selectById(5L)).thenReturn(outbox());
        when(recipientMapper.selectRecipientIds(eq(5L), anyInt(), anyInt())).thenReturn(List.of(8));
        when(explicitRecipientValidator.validate(eq(1), anyList())).thenReturn(List.of(8));
        when(channelDispatcher.dispatch(any(), anyList())).thenReturn(new DispatchOutcome(List.of(
                new ChannelPersistResult(NotificationChannel.IN_APP, true, 1, null),
                new ChannelPersistResult(NotificationChannel.IMMEDIATE_EMAIL, false, 0, "fail")
        )));
        when(outboxMapper.markRetryable(eq(5L), anyString(), any(), any(), any())).thenReturn(1);

        worker.processOne(5L);

        verify(outboxMapper).markRetryable(eq(5L), anyString(), any(), anyString(), any());
        verify(outboxMapper, never()).markDone(any(), any(), any());
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
