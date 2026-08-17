package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventRecipient;
import com.coursistant.lms.module.interaction.notification.enums.OutboxStatus;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationSupport;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.service.NotificationPolicy;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Component
public class NotificationPublisher {

    @Resource
    private NotificationEventOutboxMapper outboxMapper;

    @Resource
    private NotificationEventRecipientMapper recipientMapper;

    @Resource
    private NotificationJson notificationJson;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationProperties notificationProperties;

    @Resource
    private NotificationSupport notificationSupport;

    @Resource
    private ObjectProvider<NotificationEventRelayWorker> relayWorker;

    public Long publishInTransaction(NotificationDispatchPayload payload) {
        if (payload == null) {
            return null;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Notification outbox must be written inside a business transaction");
        }
        if (payload.getNotificationType() == null || payload.getTenantId() == null
                || payload.getCourseId() == null || payload.getSubjectType() == null
                || payload.getSubjectId() == null || payload.getEventKey() == null
                || payload.getMessage() == null || payload.getDeepLink() == null) {
            throw new IllegalArgumentException("Incomplete notification event");
        }
        if (payload.getRecipientMode() != RecipientMode.EXPLICIT) {
            throw new IllegalArgumentException("New notification events must use EXPLICIT recipient snapshots");
        }
        if (payload.getRecipientIds() == null) {
            throw new IllegalArgumentException("recipientIds is required (empty snapshot allowed)");
        }
        NotificationPolicy.forType(payload.getNotificationType());
        RecipientMode mode = RecipientMode.EXPLICIT;

        String candidateEventId = UUID.randomUUID().toString();
        payload.setEventId(candidateEventId);
        LocalDateTime now = notificationTimeSupport.nowUtc();
        NotificationEventOutbox row = new NotificationEventOutbox();
        row.setEventId(candidateEventId);
        row.setTenantId(payload.getTenantId());
        row.setCourseId(payload.getCourseId());
        row.setNotificationType(payload.getNotificationType().name());
        row.setSubjectType(payload.getSubjectType().name());
        row.setSubjectId(payload.getSubjectId());
        row.setEventKey(payload.getEventKey());
        row.setActorUserId(payload.getActorUserId());
        row.setMessage(payload.getMessage());
        row.setDeepLink(payload.getDeepLink());
        row.setOccurredAt(payload.getCreatedAt() != null ? payload.getCreatedAt() : now);
        row.setRecipientMode(mode.name());
        row.setTemplateVarsJson(notificationJson.writeVars(payload.getTemplateVars()));
        row.setStatus(OutboxStatus.PENDING.name());
        row.setNextAttemptAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);

        outboxMapper.insertIgnoreDuplicate(row);
        NotificationEventOutbox persisted = outboxMapper.selectByDedupeKey(
                payload.getTenantId(), payload.getNotificationType().name(), payload.getSubjectType().name(),
                payload.getSubjectId(), payload.getEventKey());
        if (persisted == null) {
            throw new IllegalStateException("Outbox row missing after upsert");
        }
        if (candidateEventId.equals(persisted.getEventId()) && mode == RecipientMode.EXPLICIT) {
            insertRecipients(persisted.getId(), payload.getRecipientIds());
        }
        payload.setEventId(persisted.getEventId());
        Long outboxId = persisted.getId();
        if (notificationProperties.getOutbox().isEnabled()) {
            notificationSupport.afterCommit(() -> {
                NotificationEventRelayWorker worker = relayWorker.getIfAvailable();
                if (worker != null) {
                    worker.triggerFastPath(outboxId);
                }
            });
        }
        return outboxId;
    }

    private void insertRecipients(Long outboxId, List<Integer> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (Integer id : recipientIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        int chunk = Math.max(1, notificationProperties.getOutbox().getRecipientInsertChunk());
        List<NotificationEventRecipient> buffer = new ArrayList<>(chunk);
        for (Integer userId : unique) {
            NotificationEventRecipient rec = new NotificationEventRecipient();
            rec.setOutboxId(outboxId);
            rec.setRecipientUserId(userId);
            buffer.add(rec);
            if (buffer.size() >= chunk) {
                recipientMapper.insertChunk(buffer);
                buffer = new ArrayList<>(chunk);
            }
        }
        if (!buffer.isEmpty()) {
            recipientMapper.insertChunk(buffer);
        }
    }
}
