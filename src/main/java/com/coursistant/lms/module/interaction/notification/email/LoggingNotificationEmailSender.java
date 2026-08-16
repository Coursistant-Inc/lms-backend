package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.support.EmailMasker;
import com.coursistant.lms.module.interaction.notification.support.NotificationLog;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "lms.notification.email.provider", havingValue = "log", matchIfMissing = true)
public class LoggingNotificationEmailSender implements NotificationEmailSender {

    @Resource
    private NotificationProperties notificationProperties;

    @Override
    public EmailSendResult send(EmailMessage message) {
        String id = "log-" + UUID.randomUUID();
        NotificationLog.info("email_dry_run", null, null, null, "IMMEDIATE_EMAIL", "DRY_RUN",
                null, id, message == null ? null : message.recipientUserId(), null, null);
        NotificationLog.logger().info("notification event=email_dry_run recipientUserId={} to={} subject={}",
                message == null ? null : message.recipientUserId(),
                EmailMasker.mask(message == null ? null : message.to()),
                message == null ? null : message.subject());
        return EmailSendResult.dryRun(id);
    }
}
