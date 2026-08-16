package com.coursistant.lms.module.interaction.notification.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingNotificationEmailSenderTest {

    @Test
    void send_returnsDryRunNotSent() {
        LoggingNotificationEmailSender sender = new LoggingNotificationEmailSender();
        EmailSendResult result = sender.send(new EmailMessage(4, "a@b.com", "Hello", "Body"));
        assertEquals(EmailSendStatus.DRY_RUN, result.status());
        assertTrue(result.providerMessageId() != null && result.providerMessageId().startsWith("log-"));
    }
}
