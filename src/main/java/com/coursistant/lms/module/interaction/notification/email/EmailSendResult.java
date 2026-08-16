package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;

public record EmailSendResult(EmailSendStatus status, String providerMessageId,
                              FailureCategory failureCategory, String errorMessage) {

    public static EmailSendResult sent(String providerMessageId) {
        return new EmailSendResult(EmailSendStatus.SENT, providerMessageId, null, null);
    }

    public static EmailSendResult dryRun(String providerMessageId) {
        return new EmailSendResult(EmailSendStatus.DRY_RUN, providerMessageId, null, null);
    }

    public static EmailSendResult retryable(FailureCategory category, String error) {
        return new EmailSendResult(EmailSendStatus.RETRYABLE_FAILURE, null, category, error);
    }

    public static EmailSendResult permanent(FailureCategory category, String error) {
        return new EmailSendResult(EmailSendStatus.PERMANENT_FAILURE, null, category, error);
    }
}
