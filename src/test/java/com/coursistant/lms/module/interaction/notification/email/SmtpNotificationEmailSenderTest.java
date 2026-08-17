package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import jakarta.mail.SendFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.net.ConnectException;
import java.net.SocketException;
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
    void writeTimeout_isUnknownOutcome() {
        MailSendException error = new MailSendException("Failed messages",
                new SocketTimeoutException("Write timed out"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.UNKNOWN_OUTCOME, result.status());
        assertEquals(FailureCategory.UNKNOWN_OUTCOME, result.failureCategory());
        assertEquals("smtp-write-timeout", result.errorMessage());
    }

    @Test
    void connectionResetAfterConnect_isUnknownOutcome() {
        MailSendException error = new MailSendException("Failed messages",
                new SocketException("Connection reset"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.UNKNOWN_OUTCOME, result.status());
        assertEquals("smtp-connection-lost", result.errorMessage());
    }

    @Test
    void eofAfterConnect_isUnknownOutcome() {
        MailSendException error = new MailSendException("Failed messages",
                new SocketException("Unexpected EOF on socket"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.UNKNOWN_OUTCOME, result.status());
        assertEquals("smtp-connection-lost", result.errorMessage());
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
    void smtp4xx_isRetryable() {
        MailSendException error = new MailSendException("Failed messages",
                new SendFailedException("452 4.2.2 mailbox full"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.RETRYABLE_FAILURE, result.status());
        assertEquals(FailureCategory.RETRYABLE_PROVIDER_5XX, result.failureCategory());
        assertEquals("smtp-4xx", result.errorMessage());
    }

    @Test
    void smtp5xx_isPermanent() {
        MailSendException error = new MailSendException("Failed messages",
                new SendFailedException("550 5.1.1 user unknown"));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.PERMANENT_FAILURE, result.status());
        assertEquals(FailureCategory.PERMANENT_INVALID_EMAIL, result.failureCategory());
        assertEquals("smtp-5xx", result.errorMessage());
    }

    @Test
    void smtpReturnCodeMethod_isHonored() {
        MailSendException error = new MailSendException("Failed messages", new SmtpCodedException(421));
        EmailSendResult result = sender.mapTransportFailure(error, "send-failed");
        assertEquals(EmailSendStatus.RETRYABLE_FAILURE, result.status());
        assertEquals("smtp-4xx", result.errorMessage());
    }

    private static final class SmtpCodedException extends Exception {
        private final int returnCode;

        private SmtpCodedException(int returnCode) {
            super("SMTP code " + returnCode);
            this.returnCode = returnCode;
        }

        public int getReturnCode() {
            return returnCode;
        }
    }
}
