package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.digest.TenantTimeZoneResolver;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.email.NotificationContactLookup;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.entity.UserNotification;
import com.coursistant.lms.module.interaction.notification.enums.DeliveryStatus;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.UserNotificationMapper;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.user.account.entity.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationFanoutService {

    static final int CHUNK_SIZE = 500;

    @Resource
    private UserNotificationMapper userNotificationMapper;

    @Resource
    private NotificationDeliveryMapper notificationDeliveryMapper;

    @Resource
    private NotificationContactLookup contactLookup;

    @Resource
    private NotificationJson notificationJson;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationProperties notificationProperties;

    @Resource
    private TenantTimeZoneResolver tenantTimeZoneResolver;

    public void persist(NotificationDispatchPayload payload, List<Integer> recipientIds) {
        if (payload == null) {
            throw new IllegalArgumentException("Notification payload is required");
        }
        persistInApp(payload, recipientIds);
        if (!notificationProperties.getEmail().isEnabled()) {
            return;
        }
        persistEmail(payload, recipientIds);
    }

    private void persistInApp(NotificationDispatchPayload payload, List<Integer> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        LocalDateTime now = payload.getCreatedAt() != null ? payload.getCreatedAt() : notificationTimeSupport.nowUtc();
        List<UserNotification> rows = new ArrayList<>();
        for (Integer recipientId : recipientIds) {
            if (recipientId == null) {
                continue;
            }
            UserNotification row = new UserNotification();
            row.setTenantId(payload.getTenantId());
            row.setRecipientUserId(recipientId);
            row.setCourseId(payload.getCourseId());
            row.setNotificationType(payload.getNotificationType().name());
            row.setMessage(payload.getMessage());
            row.setSubjectType(payload.getSubjectType().name());
            row.setSubjectId(payload.getSubjectId());
            row.setEventKey(payload.getEventKey());
            row.setDeepLink(payload.getDeepLink());
            row.setCreatedAt(now);
            rows.add(row);
            if (rows.size() >= CHUNK_SIZE) {
                userNotificationMapper.insertChunk(rows);
                rows = new ArrayList<>();
            }
        }
        if (!rows.isEmpty()) {
            userNotificationMapper.insertChunk(rows);
        }
    }

    private void persistEmail(NotificationDispatchPayload payload, List<Integer> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        NotificationPolicy.Mapping mapping = NotificationPolicy.forType(payload.getNotificationType());
        NotificationChannel channel = mapping.emailMode() == NotificationPolicy.EmailMode.IMMEDIATE
                ? NotificationChannel.IMMEDIATE_EMAIL
                : NotificationChannel.DAILY_DIGEST;
        Map<Integer, User> contacts = contactLookup.load(recipientIds);
        LocalDateTime now = payload.getCreatedAt() != null ? payload.getCreatedAt() : notificationTimeSupport.nowUtc();
        String vars = notificationJson.writeVars(payload.getTemplateVars());
        List<NotificationDelivery> rows = new ArrayList<>();
        for (Integer recipientId : recipientIds) {
            if (recipientId == null) {
                continue;
            }
            User user = contacts.get(recipientId);
            NotificationDelivery row = baseRow(payload, recipientId, channel, now, vars);
            if (!contactLookup.accountActive(user)) {
                row.setStatus(DeliveryStatus.FAILED_PERMANENT.name());
                row.setFailureCategory(FailureCategory.PERMANENT_RECIPIENT_INVALID.name());
            } else if (!contactLookup.emailEnabled(user)) {
                row.setStatus(DeliveryStatus.SKIPPED_PREFERENCE.name());
                row.setFailureCategory(FailureCategory.PERMANENT_PREFERENCE.name());
            } else if (!contactLookup.hasUsableEmail(user)) {
                row.setStatus(DeliveryStatus.FAILED_PERMANENT.name());
                row.setFailureCategory(FailureCategory.PERMANENT_NO_EMAIL.name());
            } else {
                row.setStatus(DeliveryStatus.PENDING.name());
            }
            if (channel == NotificationChannel.DAILY_DIGEST) {
                row.setDigestDate(tenantTimeZoneResolver.digestDate(payload.getTenantId(), now));
            }
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

    private NotificationDelivery baseRow(NotificationDispatchPayload payload, Integer recipientId,
                                         NotificationChannel channel, LocalDateTime now, String vars) {
        NotificationDelivery row = new NotificationDelivery();
        row.setEventId(payload.getEventId());
        row.setTenantId(payload.getTenantId());
        row.setRecipientUserId(recipientId);
        row.setCourseId(payload.getCourseId());
        row.setNotificationType(payload.getNotificationType().name());
        row.setSubjectType(payload.getSubjectType().name());
        row.setSubjectId(payload.getSubjectId());
        row.setEventKey(payload.getEventKey());
        row.setChannel(channel.name());
        row.setMessage(payload.getMessage());
        row.setDeepLink(payload.getDeepLink());
        row.setTemplateVarsJson(vars);
        row.setOccurredAt(now);
        row.setNextAttemptAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }
}
