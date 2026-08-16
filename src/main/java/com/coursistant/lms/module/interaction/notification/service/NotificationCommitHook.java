package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.event.NotificationEventPublisher;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Caller-facing hook: persist the notification event in the current business transaction.
 */
@Component
public class NotificationCommitHook {

    private static final Logger log = LoggerFactory.getLogger(NotificationCommitHook.class);

    @Resource
    private NotificationEventPublisher notificationEventPublisher;

    public void afterCommitDispatch(NotificationDispatchPayload payload) {
        publishInTransaction(payload);
    }

    public void publishInTransaction(NotificationDispatchPayload payload) {
        if (payload == null) {
            return;
        }
        try {
            notificationEventPublisher.publishInTransaction(payload);
        } catch (RuntimeException e) {
            log.error("Notification outbox write failed: type={} subjectType={} subjectId={} eventKey={}",
                    payload.getNotificationType(),
                    payload.getSubjectType(),
                    payload.getSubjectId(),
                    payload.getEventKey());
            throw e;
        }
    }
}
