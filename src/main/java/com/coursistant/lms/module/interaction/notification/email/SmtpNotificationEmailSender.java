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
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "lms.notification.email.provider", havingValue = "smtp")
public class SmtpNotificationEmailSender implements NotificationEmailSender {

    private static final Pattern SMTP_CODE = Pattern.compile("(?:^|\\s)([45]\\d{2})(?:\\s|$)");

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
        Integer smtpCode = extractSmtpReturnCode(e);
        if (smtpCode != null) {
            return mapSmtpResponse(smtpCode);
        }
        if (isConnectEstablishmentFailure(e)) {
            if (containsTimeout(e)) {
                return EmailSendResult.retryable(FailureCategory.RETRYABLE_TIMEOUT, "connect-timeout");
            }
            return EmailSendResult.retryable(FailureCategory.RETRYABLE_NETWORK, networkDetail);
        }
        return EmailSendResult.unknown(FailureCategory.UNKNOWN_OUTCOME, unknownDetail(e));
    }

    private EmailSendResult mapSmtpResponse(int code) {
        if (code >= 400 && code < 500) {
            return EmailSendResult.retryable(FailureCategory.RETRYABLE_PROVIDER_5XX, "smtp-4xx");
        }
        if (code >= 500 && code < 600) {
            return EmailSendResult.permanent(FailureCategory.PERMANENT_INVALID_EMAIL, "smtp-5xx");
        }
        return EmailSendResult.unknown(FailureCategory.UNKNOWN_OUTCOME, "smtp-unknown-outcome");
    }

    private Integer extractSmtpReturnCode(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            Integer fromMethod = invokeReturnCode(cur);
            if (isSmtpCode(fromMethod)) {
                return fromMethod;
            }
            if (isSmtpFailedException(cur)) {
                Integer parsed = parseSmtpCode(cur.getMessage());
                if (isSmtpCode(parsed)) {
                    return parsed;
                }
            }
            if (cur instanceof MailSendException mailSend) {
                Map<Object, Exception> failed = mailSend.getFailedMessages();
                if (failed != null) {
                    for (Exception nested : failed.values()) {
                        Integer nestedCode = extractSmtpReturnCode(nested);
                        if (isSmtpCode(nestedCode)) {
                            return nestedCode;
                        }
                    }
                }
            }
            cur = cur.getCause();
        }
        return null;
    }

    private boolean isConnectEstablishmentFailure(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof ConnectException
                    || cur instanceof UnknownHostException
                    || cur instanceof NoRouteToHostException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.endsWith("MailConnectException")) {
                return true;
            }
            String lower = messageOf(cur);
            if (lower.contains("connection refused")
                    || lower.contains("connect timed out")
                    || lower.contains("connection timed out")
                    || lower.contains("no route to host")
                    || lower.contains("unknown host")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private String unknownDetail(Throwable e) {
        if (messageContains(e, "write timed out") || messageContains(e, "writetimeout")) {
            return "smtp-write-timeout";
        }
        if (messageContains(e, "read timed out") || messageContains(e, "reading response")) {
            return "smtp-read-timeout";
        }
        if (messageContains(e, "connection reset")
                || messageContains(e, "broken pipe")
                || messageContains(e, "eof")
                || messageContains(e, "connection abort")
                || messageContains(e, "socket closed")) {
            return "smtp-connection-lost";
        }
        return "smtp-unknown-outcome";
    }

    private boolean containsTimeout(Throwable e) {
        return messageContains(e, "timed out") || messageContains(e, "timeout");
    }

    private boolean messageContains(Throwable e, String token) {
        Throwable cur = e;
        while (cur != null) {
            if (messageOf(cur).contains(token)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isSmtpFailedException(Throwable cur) {
        String name = cur.getClass().getName();
        return name.contains("SMTPSendFailed")
                || name.contains("SMTPAddressFailed")
                || name.endsWith("SendFailedException");
    }

    private static Integer invokeReturnCode(Throwable cur) {
        try {
            Object value = cur.getClass().getMethod("getReturnCode").invoke(cur);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static Integer parseSmtpCode(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = SMTP_CODE.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static boolean isSmtpCode(Integer code) {
        return code != null && code >= 400 && code < 600;
    }

    private static String messageOf(Throwable cur) {
        String msg = cur.getMessage();
        return msg == null ? "" : msg.toLowerCase();
    }
}
