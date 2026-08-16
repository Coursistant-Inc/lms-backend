package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.entity.UserNotification;
import com.coursistant.lms.module.interaction.notification.enums.DeliveryStatus;
import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.event.NotificationEvent;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.service.NotificationWriteService;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class InAppNotificationChannel {

    static final int CHUNK_SIZE = 500;

    @Resource
    private NotificationWriteService notificationWriteService;

    @Resource
    private NotificationDeliveryMapper notificationDeliveryMapper;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationJson notificationJson;

    public ChannelPersistResult persist(NotificationEvent event, List<Integer> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return new ChannelPersistResult(NotificationChannel.IN_APP, true, 0, null);
        }
        LocalDateTime now = event.getOccurredAt() != null ? event.getOccurredAt() : notificationTimeSupport.nowUtc();
        try {
            List<UserNotification> rows = new ArrayList<>();
            for (Integer recipientId : recipientIds) {
                if (recipientId == null) {
                    continue;
                }
                UserNotification row = new UserNotification();
                row.setTenantId(event.getTenantId());
                row.setRecipientUserId(recipientId);
                row.setCourseId(event.getCourseId());
                row.setNotificationType(event.getEventType().name());
                row.setMessage(event.getMessage());
                row.setSubjectType(event.getSubjectType().name());
                row.setSubjectId(event.getSubjectId());
                row.setEventKey(event.getEventKey());
                row.setDeepLink(event.getDeepLink());
                row.setCreatedAt(now);
                rows.add(row);
            }
            int written = 0;
            for (int i = 0; i < rows.size(); i += CHUNK_SIZE) {
                List<UserNotification> chunk = rows.subList(i, Math.min(i + CHUNK_SIZE, rows.size()));
                written += notificationWriteService.insertChunk(new ArrayList<>(chunk));
            }
            upsertDeliveries(event, recipientIds, now, DeliveryStatus.SENT);
            return new ChannelPersistResult(NotificationChannel.IN_APP, true, written, null);
        } catch (Exception e) {
            return new ChannelPersistResult(NotificationChannel.IN_APP, false, 0, e.getMessage());
        }
    }

    private void upsertDeliveries(NotificationEvent event, List<Integer> recipientIds,
                                  LocalDateTime now, DeliveryStatus status) {
        List<NotificationDelivery> rows = new ArrayList<>();
        String vars = notificationJson.writeVars(event.getTemplateVars());
        for (Integer recipientId : recipientIds) {
            if (recipientId == null) {
                continue;
            }
            NotificationDelivery row = new NotificationDelivery();
            row.setEventId(event.getEventId());
            row.setTenantId(event.getTenantId());
            row.setRecipientUserId(recipientId);
            row.setCourseId(event.getCourseId());
            row.setNotificationType(event.getEventType().name());
            row.setSubjectType(event.getSubjectType().name());
            row.setSubjectId(event.getSubjectId());
            row.setEventKey(event.getEventKey());
            row.setChannel(NotificationChannel.IN_APP.name());
            row.setStatus(status.name());
            row.setMessage(event.getMessage());
            row.setDeepLink(event.getDeepLink());
            row.setTemplateVarsJson(vars);
            row.setOccurredAt(now);
            row.setNextAttemptAt(now);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            rows.add(row);
            if (rows.size() >= CHUNK_SIZE) {
                notificationDeliveryMapper.upsertChunk(rows);
                rows = new ArrayList<>();
            }
        }
        if (!rows.isEmpty()) {
            notificationDeliveryMapper.upsertChunk(rows);
        }
    }
}
