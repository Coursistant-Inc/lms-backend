package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}
