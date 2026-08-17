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

import java.net.ConnectException;
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
            return mapTransportFailure(e, "send-failed");
        } catch (Exception e) {
            return mapTransportFailure(e, "provider-error");
        }
    }

    EmailSendResult mapTransportFailure(Throwable e, String networkDetail) {
        if (isConnectFailure(e)) {
            if (containsTimeout(e)) {
                return EmailSendResult.retryable(FailureCategory.RETRYABLE_TIMEOUT, "connect-timeout");
            }
            return EmailSendResult.retryable(FailureCategory.RETRYABLE_NETWORK, networkDetail);
        }
        if (isWriteTimeout(e)) {
            return EmailSendResult.retryable(FailureCategory.RETRYABLE_TIMEOUT, "write-timeout");
        }
        if (containsTimeout(e)) {
            return EmailSendResult.unknown(FailureCategory.UNKNOWN_OUTCOME, "smtp-read-timeout");
        }
        return EmailSendResult.retryable(FailureCategory.RETRYABLE_NETWORK, networkDetail);
    }

    private boolean isConnectFailure(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof ConnectException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.endsWith("MailConnectException")) {
                return true;
            }
            String lower = messageOf(cur);
            if (lower.contains("connect timed out")
                    || lower.contains("connection timed out")
                    || lower.contains("connection refused")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private boolean isWriteTimeout(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String lower = messageOf(cur);
            if (lower.contains("write timed out") || lower.contains("writetimeout")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private boolean containsTimeout(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof SocketTimeoutException) {
                return true;
            }
            String lower = messageOf(cur);
            if (lower.contains("timed out") || lower.contains("timeout")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String messageOf(Throwable cur) {
        String msg = cur.getMessage();
        return msg == null ? "" : msg.toLowerCase();
    }
}
