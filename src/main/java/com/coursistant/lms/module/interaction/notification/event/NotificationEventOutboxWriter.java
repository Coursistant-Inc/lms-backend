package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelRouter;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventRecipient;
import com.coursistant.lms.module.interaction.notification.enums.OutboxStatus;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Component
public class NotificationEventOutboxWriter {

    @Resource
    private NotificationEventOutboxMapper outboxMapper;

    @Resource
    private NotificationEventRecipientMapper recipientMapper;

    @Resource
    private NotificationChannelRouter channelRouter;

    @Resource
    private NotificationJson notificationJson;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationProperties notificationProperties;

    public Long write(NotificationEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Notification outbox must be written inside a business transaction");
        }
        if (event == null || event.getEventType() == null || event.getTenantId() == null
                || event.getCourseId() == null || event.getSubjectType() == null
                || event.getSubjectId() == null || event.getEventKey() == null
                || event.getMessage() == null || event.getDeepLink() == null) {
            throw new IllegalArgumentException("Incomplete notification event");
        }
        RecipientMode mode = event.getRecipientMode() != null
                ? event.getRecipientMode()
                : channelRouter.requiredRecipientMode(event.getEventType());
        channelRouter.assertRecipientMode(event.getEventType(), mode);

        String eventId = event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString();
        event.setEventId(eventId);
        LocalDateTime now = notificationTimeSupport.nowUtc();
        NotificationEventOutbox row = new NotificationEventOutbox();
        row.setEventId(eventId);
        row.setTenantId(event.getTenantId());
        row.setCourseId(event.getCourseId());
        row.setNotificationType(event.getEventType().name());
        row.setSubjectType(event.getSubjectType().name());
        row.setSubjectId(event.getSubjectId());
        row.setEventKey(event.getEventKey());
        row.setActorUserId(event.getActorUserId());
        row.setMessage(event.getMessage());
        row.setDeepLink(event.getDeepLink());
        row.setOccurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : now);
        row.setRecipientMode(mode.name());
        row.setTemplateVarsJson(notificationJson.writeVars(event.getTemplateVars()));
        row.setStatus(OutboxStatus.PENDING.name());
        row.setNextAttemptAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);

        int rows = outboxMapper.insertIgnoreDuplicate(row);
        if (rows == 1) {
            if (mode == RecipientMode.EXPLICIT) {
                insertRecipients(row.getId(), event.getRecipientIds());
            }
            return row.getId();
        }
        NotificationEventOutbox existing = outboxMapper.selectByDedupeKey(
                event.getTenantId(), event.getEventType().name(), event.getSubjectType().name(),
                event.getSubjectId(), event.getEventKey());
        if (existing == null) {
            throw new IllegalStateException("Outbox duplicate detected but existing row not found");
        }
        return existing.getId();
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
