package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelDispatcher;
import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelRouter;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.service.NotificationSupport;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    @Resource
    private NotificationEventOutboxWriter outboxWriter;

    @Resource
    private NotificationSupport notificationSupport;

    @Resource
    private NotificationChannelRouter channelRouter;

    @Resource
    private NotificationChannelDispatcher channelDispatcher;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationProperties notificationProperties;

    @Resource
    private ObjectProvider<NotificationEventRelayWorker> relayWorker;

    public Long publishInTransaction(NotificationDispatchPayload payload) {
        if (payload == null) {
            return null;
        }
        NotificationEvent event = fromPayload(payload);
        if (notificationProperties.getOutbox().isEnabled()) {
            Long id = outboxWriter.write(event);
            notificationSupport.afterCommit(() -> {
                NotificationEventRelayWorker worker = relayWorker.getIfAvailable();
                if (worker != null) {
                    worker.triggerFastPath(id);
                }
            });
            return id;
        }
        notificationSupport.afterCommit(() -> channelDispatcher.dispatch(event, event.getRecipientIds()));
        return null;
    }

    public Long publishInTransaction(NotificationEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getRecipientMode() == null) {
            event.setRecipientMode(channelRouter.requiredRecipientMode(event.getEventType()));
        }
        channelRouter.assertRecipientMode(event.getEventType(), event.getRecipientMode());
        if (notificationProperties.getOutbox().isEnabled()) {
            Long id = outboxWriter.write(event);
            notificationSupport.afterCommit(() -> {
                NotificationEventRelayWorker worker = relayWorker.getIfAvailable();
                if (worker != null) {
                    worker.triggerFastPath(id);
                }
            });
            return id;
        }
        notificationSupport.afterCommit(() -> channelDispatcher.dispatch(event, event.getRecipientIds()));
        return null;
    }

    private NotificationEvent fromPayload(NotificationDispatchPayload payload) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(payload.getNotificationType());
        event.setEventKey(payload.getEventKey());
        event.setTenantId(payload.getTenantId());
        event.setCourseId(payload.getCourseId());
        event.setActorUserId(payload.getActorUserId());
        RecipientMode mode = payload.getRecipientMode();
        if (mode == null) {
            mode = channelRouter.requiredRecipientMode(payload.getNotificationType());
        }
        event.setRecipientMode(mode);
        event.setRecipientIds(payload.getRecipientIds());
        event.setSubjectType(payload.getSubjectType());
        event.setSubjectId(payload.getSubjectId());
        event.setMessage(payload.getMessage());
        event.setDeepLink(payload.getDeepLink());
        event.setOccurredAt(payload.getCreatedAt() != null ? payload.getCreatedAt() : notificationTimeSupport.nowUtc());
        event.setTemplateVars(payload.getTemplateVars());
        return event;
    }
}
