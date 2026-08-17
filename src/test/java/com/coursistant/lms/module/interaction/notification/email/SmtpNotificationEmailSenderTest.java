package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmtpNotificationEmailSenderTest {

    private final SmtpNotificationEmailSender sender = new SmtpNotificationEmailSender();

    @Test
    void readTimeout_isUnknownOutcome() {
        MailSendException error = new MailSendException("Failed messages",
                new SocketTimeoutException("Read timed out"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.UNKNOWN_OUTCOME, result.status());
        assertEquals(FailureCategory.UNKNOWN_OUTCOME, result.failureCategory());
        assertEquals("smtp-read-timeout", result.errorMessage());
    }

    @Test
    void connectTimeout_isRetryable() {
        MailSendException error = new MailSendException("Failed messages",
                new SocketTimeoutException("Connect timed out"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.RETRYABLE_FAILURE, result.status());
        assertEquals(FailureCategory.RETRYABLE_TIMEOUT, result.failureCategory());
        assertEquals("connect-timeout", result.errorMessage());
    }

    @Test
    void connectionRefused_isRetryableNetwork() {
        MailSendException error = new MailSendException("Failed messages",
                new ConnectException("Connection refused"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.RETRYABLE_FAILURE, result.status());
        assertEquals(FailureCategory.RETRYABLE_NETWORK, result.failureCategory());
        assertEquals("send-failed", result.errorMessage());
    }

    @Test
    void writeTimeout_isRetryable() {
        MailSendException error = new MailSendException("Failed messages",
                new SocketTimeoutException("Write timed out"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.RETRYABLE_FAILURE, result.status());
        assertEquals(FailureCategory.RETRYABLE_TIMEOUT, result.failureCategory());
        assertEquals("write-timeout", result.errorMessage());
    }
}
