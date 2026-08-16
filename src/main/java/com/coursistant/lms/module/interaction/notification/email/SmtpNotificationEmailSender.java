package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import jakarta.annotation.Resource;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;

@Component
@ConditionalOnProperty(name = "lms.notification.email.provider", havingValue = "smtp")
public class SmtpNotificationEmailSender implements NotificationEmailSender {

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private NotificationProperties notificationProperties;

    @Override
    public EmailSendResult send(EmailMessage message) {
        if (message == null || message.to() == null || !message.to().contains("@")) {
            return EmailSendResult.permanent(FailureCategory.PERMANENT_NO_EMAIL, "missing-email");
        }
        try {
            InternetAddress.parse(message.to(), true);
            MimeMessage mime = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            String from = notificationProperties.getEmail().getFromAddress();
            String name = notificationProperties.getEmail().getFromName();
            if (name == null || name.isBlank()) {
                helper.setFrom(from);
            } else {
                helper.setFrom(from, name);
            }
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.textBody() == null ? "" : message.textBody(), false);
            javaMailSender.send(mime);
            String id = mime.getMessageID();
            return EmailSendResult.sent(id != null ? id : "smtp-accepted");
        } catch (AddressException e) {
            return EmailSendResult.permanent(FailureCategory.PERMANENT_INVALID_EMAIL, "invalid-address");
        } catch (MailAuthenticationException e) {
            return EmailSendResult.retryable(FailureCategory.RETRYABLE_PROVIDER_5XX, "auth-failed");
        } catch (MailSendException e) {
            if (isTimeout(e)) {
                return EmailSendResult.retryable(FailureCategory.RETRYABLE_TIMEOUT, "timeout");
            }
            return EmailSendResult.retryable(FailureCategory.RETRYABLE_NETWORK, "send-failed");
        } catch (Exception e) {
            if (isTimeout(e)) {
                return EmailSendResult.retryable(FailureCategory.RETRYABLE_TIMEOUT, "timeout");
            }
            return EmailSendResult.retryable(FailureCategory.RETRYABLE_NETWORK, "provider-error");
        }
    }

    private boolean isTimeout(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof SocketTimeoutException) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null && msg.toLowerCase().contains("timed out")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
