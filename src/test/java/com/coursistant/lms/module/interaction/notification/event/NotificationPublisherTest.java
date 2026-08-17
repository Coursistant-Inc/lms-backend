package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationSupport;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock private NotificationEventOutboxMapper outboxMapper;
    @Mock private NotificationEventRecipientMapper recipientMapper;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @Mock private NotificationSupport notificationSupport;
    @Mock private ObjectProvider<NotificationEventRelayWorker> relayWorker;
    @InjectMocks private NotificationPublisher publisher;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "notificationJson",
                new NotificationJson(new ObjectMapper()));
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "notificationProperties",
                new NotificationProperties());
        org.mockito.Mockito.lenient().when(notificationTimeSupport.nowUtc())
                .thenReturn(LocalDateTime.of(2026, 8, 16, 1, 0));
    }

    @Test
    void duplicateEvent_doesNotRewriteRecipients() {
        NotificationDispatchPayload payload = payload(List.of(1, 2, 3));
        payload.setEventId("caller-supplied");
        NotificationEventOutbox existing = new NotificationEventOutbox();
        existing.setId(44L);
        existing.setEventId("already-persisted");
        when(outboxMapper.selectByDedupeKey(1, "ASSIGNMENT_GRADE_RELEASED", "ASSIGNMENT", 9, "release:7"))
                .thenReturn(existing);

        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            assertEquals(44L, publisher.publishInTransaction(payload));
        }
        verify(recipientMapper, never()).insertChunk(anyList());
        assertEquals("already-persisted", payload.getEventId());
        assertNotEquals("caller-supplied", payload.getEventId());
    }

    @Test
    void newEvent_writesRecipientsAndIgnoresCallerEventId() {
        NotificationDispatchPayload payload = payload(List.of(1, 2));
        payload.setEventId("caller-supplied");
        when(outboxMapper.selectByDedupeKey(1, "ASSIGNMENT_GRADE_RELEASED", "ASSIGNMENT", 9, "release:7"))
                .thenAnswer(inv -> {
                    NotificationEventOutbox persisted = new NotificationEventOutbox();
                    persisted.setId(10L);
                    persisted.setEventId(payload.getEventId());
                    return persisted;
                });
        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            assertEquals(10L, publisher.publishInTransaction(payload));
        }
        verify(recipientMapper).insertChunk(anyList());
        assertNotEquals("caller-supplied", payload.getEventId());
        verify(notificationSupport).afterCommit(any());
    }

    @Test
    void outboxDisabled_stillWritesAndSkipsFastPath() {
        NotificationProperties properties = new NotificationProperties();
        properties.getOutbox().setEnabled(false);
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "notificationProperties", properties);
        NotificationDispatchPayload payload = payload(List.of(1));
        when(outboxMapper.selectByDedupeKey(1, "ASSIGNMENT_GRADE_RELEASED", "ASSIGNMENT", 9, "release:7"))
                .thenAnswer(inv -> {
                    NotificationEventOutbox persisted = new NotificationEventOutbox();
                    persisted.setId(10L);
                    persisted.setEventId(payload.getEventId());
                    return persisted;
                });
        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            assertEquals(10L, publisher.publishInTransaction(payload));
        }
        verify(recipientMapper).insertChunk(anyList());
        verify(notificationSupport, never()).afterCommit(any());
        verify(relayWorker, never()).getIfAvailable();
    }

    @Test
    void emptySnapshot_isAllowedAndDoesNotInsertRecipients() {
        NotificationDispatchPayload payload = payload(List.of());
        when(outboxMapper.selectByDedupeKey(1, "ASSIGNMENT_GRADE_RELEASED", "ASSIGNMENT", 9, "release:7"))
                .thenAnswer(inv -> {
                    NotificationEventOutbox persisted = new NotificationEventOutbox();
                    persisted.setId(10L);
                    persisted.setEventId(payload.getEventId());
                    return persisted;
                });
        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            assertEquals(10L, publisher.publishInTransaction(payload));
        }
        verify(recipientMapper, never()).insertChunk(anyList());
    }

    @Test
    void nonExplicitMode_isRejected() {
        NotificationDispatchPayload payload = payload(List.of(1));
        payload.setRecipientMode(RecipientMode.COURSE_ACTIVE_STUDENTS);
        try (MockedStatic<TransactionSynchronizationManager> tx = mockStatic(TransactionSynchronizationManager.class)) {
            tx.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> publisher.publishInTransaction(payload));
        }
        verify(outboxMapper, never()).insertIgnoreDuplicate(any());
    }

    private NotificationDispatchPayload payload(List<Integer> recipients) {
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setNotificationType(NotificationType.ASSIGNMENT_GRADE_RELEASED);
        payload.setTenantId(1);
        payload.setCourseId(2);
        payload.setSubjectType(SubjectType.ASSIGNMENT);
        payload.setSubjectId(9);
        payload.setEventKey("release:7");
        payload.setMessage("released");
        payload.setDeepLink("/x");
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setRecipientIds(recipients);
        return payload;
    }
}
