package com.coursistant.lms.module.interaction.notification.email;

public interface NotificationEmailSender {

    EmailSendResult send(EmailMessage message);
}
