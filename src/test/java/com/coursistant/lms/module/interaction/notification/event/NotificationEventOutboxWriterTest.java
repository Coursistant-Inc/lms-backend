package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelRouter;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventOutboxWriterTest {

    @Mock private NotificationEventOutboxMapper outboxMapper;
    @Mock private NotificationEventRecipientMapper recipientMapper;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @InjectMocks private NotificationEventOutboxWriter writer;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "channelRouter", new NotificationChannelRouter());
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "notificationJson", new NotificationJson(new ObjectMapper()));
        org.springframework.test.util.ReflectionTestUtils.setField(writer, "notificationProperties", new NotificationProperties());
        when(notificationTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 16, 1, 0));
    }

    @Test
    void duplicateEvent_doesNotRewriteRecipients() {
        NotificationEvent event = event(List.of(1, 2, 3));
        when(outboxMapper.insertIgnoreDuplicate(any())).thenReturn(2);
        NotificationEventOutbox existing = new NotificationEventOutbox();
        existing.setId(44L);
        when(outboxMapper.selectByDedupeKey(1, "ASSIGNMENT_GRADE_RELEASED", "ASSIGNMENT", 9, "release:7"))
                .thenReturn(existing);

        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            assertEquals(44L, writer.write(event));
        }
        verify(recipientMapper, never()).insertChunk(anyList());
    }

    @Test
    void newEvent_writesRecipients() {
        NotificationEvent event = event(List.of(1, 2));
        when(outboxMapper.insertIgnoreDuplicate(any())).thenAnswer(inv -> {
            NotificationEventOutbox row = inv.getArgument(0);
            row.setId(10L);
            return 1;
        });
        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            assertEquals(10L, writer.write(event));
        }
        verify(recipientMapper).insertChunk(anyList());
    }

    private NotificationEvent event(List<Integer> recipients) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(NotificationType.ASSIGNMENT_GRADE_RELEASED);
        event.setTenantId(1);
        event.setCourseId(2);
        event.setSubjectType(SubjectType.ASSIGNMENT);
        event.setSubjectId(9);
        event.setEventKey("release:7");
        event.setMessage("released");
        event.setDeepLink("/x");
        event.setRecipientMode(RecipientMode.EXPLICIT);
        event.setRecipientIds(recipients);
        return event;
    }
}
