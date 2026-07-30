package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Caller-facing hook: schedule async dispatch after the business transaction commits.
 * Invokes {@link NotificationDispatcher#dispatchAsync} via a separate bean so {@code @Async} applies.
 */
@Component
public class NotificationCommitHook {

    private static final Logger log = LoggerFactory.getLogger(NotificationCommitHook.class);

    @Resource
    private NotificationSupport notificationSupport;

    @Resource
    private NotificationDispatcher notificationDispatcher;

    public void afterCommitDispatch(NotificationDispatchPayload payload) {
        if (payload == null) {
            return;
        }
        notificationSupport.afterCommit(() -> {
            try {
                notificationDispatcher.dispatchAsync(payload);
            } catch (Exception e) {
                log.warn("Notification async submit failed (ignored): type={}, subjectType={}, subjectId={}, eventKey={}, error={}",
                        payload.getNotificationType(),
                        payload.getSubjectType(),
                        payload.getSubjectId(),
                        payload.getEventKey(),
                        e.getMessage());
            }
        });
    }
}
