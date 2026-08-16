package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.digest.TenantTimeZoneResolver;
import com.coursistant.lms.module.interaction.notification.email.NotificationContactLookup;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.enums.DeliveryStatus;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.event.NotificationEvent;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.user.account.entity.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmailQueueChannel {

    private static final int CHUNK = 500;

    @Resource
    private NotificationDeliveryMapper notificationDeliveryMapper;

    @Resource
    private NotificationContactLookup contactLookup;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationJson notificationJson;

    @Resource
    private NotificationProperties notificationProperties;

    @Resource
    private TenantTimeZoneResolver tenantTimeZoneResolver;

    public ChannelPersistResult persistImmediate(NotificationEvent event, List<Integer> recipientIds) {
        return persist(event, recipientIds, NotificationChannel.IMMEDIATE_EMAIL);
    }

    public ChannelPersistResult persistDigest(NotificationEvent event, List<Integer> recipientIds) {
        return persist(event, recipientIds, NotificationChannel.DAILY_DIGEST);
    }

    private ChannelPersistResult persist(NotificationEvent event, List<Integer> recipientIds,
                                         NotificationChannel channel) {
        if (!notificationProperties.getEmail().isEnabled()) {
            return new ChannelPersistResult(channel, true, 0, null);
        }
        if (recipientIds == null || recipientIds.isEmpty()) {
            return new ChannelPersistResult(channel, true, 0, null);
        }
        try {
            Map<Integer, User> contacts = contactLookup.load(recipientIds);
            LocalDateTime now = event.getOccurredAt() != null ? event.getOccurredAt() : notificationTimeSupport.nowUtc();
            String vars = notificationJson.writeVars(event.getTemplateVars());
            List<NotificationDelivery> rows = new ArrayList<>();
            int count = 0;
            for (Integer recipientId : recipientIds) {
                if (recipientId == null) {
                    continue;
                }
                User user = contacts.get(recipientId);
                NotificationDelivery row = baseRow(event, recipientId, channel, now, vars);
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
                    row.setDigestDate(tenantTimeZoneResolver.digestDate(event.getTenantId(), now));
                }
                rows.add(row);
                count++;
                if (rows.size() >= CHUNK) {
                    notificationDeliveryMapper.upsertChunk(rows);
                    rows = new ArrayList<>();
                }
            }
            if (!rows.isEmpty()) {
                notificationDeliveryMapper.upsertChunk(rows);
            }
            return new ChannelPersistResult(channel, true, count, null);
        } catch (Exception e) {
            return new ChannelPersistResult(channel, false, 0, e.getMessage());
        }
    }

    private NotificationDelivery baseRow(NotificationEvent event, Integer recipientId,
                                         NotificationChannel channel, LocalDateTime now, String vars) {
        NotificationDelivery row = new NotificationDelivery();
        row.setEventId(event.getEventId());
        row.setTenantId(event.getTenantId());
        row.setRecipientUserId(recipientId);
        row.setCourseId(event.getCourseId());
        row.setNotificationType(event.getEventType().name());
        row.setSubjectType(event.getSubjectType().name());
        row.setSubjectId(event.getSubjectId());
        row.setEventKey(event.getEventKey());
        row.setChannel(channel.name());
        row.setMessage(event.getMessage());
        row.setDeepLink(event.getDeepLink());
        row.setTemplateVarsJson(vars);
        row.setOccurredAt(now);
        row.setNextAttemptAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return row;
    }
}
