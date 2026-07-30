package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.entity.UserNotification;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fire-and-forget async dispatch. Chunks recipients and writes each chunk via
 * {@link NotificationWriteService#insertChunk} (Spring proxy / REQUIRES_NEW).
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final int CHUNK_SIZE = 500;

    @Resource
    private NotificationWriteService notificationWriteService;

    @Async("notificationExecutor")
    public void dispatchAsync(NotificationDispatchPayload payload) {
        if (payload == null) {
            return;
        }
        try {
            List<UserNotification> rows = buildRows(payload);
            if (rows.isEmpty()) {
                return;
            }
            for (int i = 0; i < rows.size(); i += CHUNK_SIZE) {
                List<UserNotification> chunk = rows.subList(i, Math.min(i + CHUNK_SIZE, rows.size()));
                notificationWriteService.insertChunk(new ArrayList<>(chunk));
            }
        } catch (Exception e) {
            log.warn("Notification dispatch failed (ignored): type={}, subjectType={}, subjectId={}, eventKey={}, error={}",
                    payload.getNotificationType(),
                    payload.getSubjectType(),
                    payload.getSubjectId(),
                    payload.getEventKey(),
                    e.getMessage());
        }
    }

    private List<UserNotification> buildRows(NotificationDispatchPayload payload) {
        List<UserNotification> rows = new ArrayList<>();
        List<Integer> recipientIds = payload.getRecipientIds();
        if (recipientIds == null || recipientIds.isEmpty()) {
            return rows;
        }
        if (payload.getTenantId() == null
                || payload.getCourseId() == null
                || payload.getNotificationType() == null
                || payload.getSubjectType() == null
                || payload.getSubjectId() == null
                || payload.getEventKey() == null
                || payload.getMessage() == null
                || payload.getDeepLink() == null) {
            log.warn("Notification dispatch skipped (incomplete payload): type={}, subjectType={}, subjectId={}, eventKey={}",
                    payload.getNotificationType(),
                    payload.getSubjectType(),
                    payload.getSubjectId(),
                    payload.getEventKey());
            return rows;
        }
        LocalDateTime createdAt = payload.getCreatedAt() != null ? payload.getCreatedAt() : LocalDateTime.now();
        String type = payload.getNotificationType().name();
        String subjectType = payload.getSubjectType().name();
        for (Integer recipientId : recipientIds) {
            if (recipientId == null) {
                continue;
            }
            UserNotification row = new UserNotification();
            row.setTenantId(payload.getTenantId());
            row.setRecipientUserId(recipientId);
            row.setCourseId(payload.getCourseId());
            row.setNotificationType(type);
            row.setMessage(payload.getMessage());
            row.setSubjectType(subjectType);
            row.setSubjectId(payload.getSubjectId());
            row.setEventKey(payload.getEventKey());
            row.setDeepLink(payload.getDeepLink());
            row.setCreatedAt(createdAt);
            rows.add(row);
        }
        return rows;
    }
}
