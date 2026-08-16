package com.coursistant.lms.module.interaction.notification.email;

public record EmailMessage(Integer recipientUserId, String to, String subject, String textBody) {
}
