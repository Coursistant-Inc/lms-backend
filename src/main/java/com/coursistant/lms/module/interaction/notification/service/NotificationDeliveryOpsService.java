package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationDeliveryOpsService {

    @Resource
    private NotificationDeliveryMapper deliveryMapper;

    @Resource
    private NotificationDigestEmailMapper digestEmailMapper;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    public void cancelPendingEmailsFor(Integer userId) {
        if (userId == null) {
            return;
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        deliveryMapper.cancelPendingEmailsForRecipient(userId, now);
        digestEmailMapper.cancelPendingForRecipient(userId, now);
    }

    @Transactional
    public int retryDelivery(Long id) {
        return deliveryMapper.requeueDelivery(id, notificationTimeSupport.nowUtc());
    }

    @Transactional
    public int requeueDryRun(LocalDateTime from, LocalDateTime to, Integer tenantId, String channel) {
        LocalDateTime now = notificationTimeSupport.nowUtc();
        int deliveries = deliveryMapper.requeueDryRunInRange(from, to, tenantId, channel, now);
        List<Long> digestIds = digestEmailMapper.selectDryRunOrPermanentIds(from, to, tenantId);
        int digests = digestEmailMapper.requeueDryRunInRange(from, to, tenantId, now);
        if (digestIds != null && !digestIds.isEmpty()) {
            deliveryMapper.restoreItemsForDigestEmails(digestIds);
        }
        return deliveries + digests;
    }
}
