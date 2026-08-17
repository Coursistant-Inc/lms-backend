package com.coursistant.lms.module.interaction.notification.email;

public enum EmailSendStatus {
    SENT,
    DRY_RUN,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE,
    UNKNOWN_OUTCOME
}
