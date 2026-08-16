package com.coursistant.lms.module.interaction.notification.enums;

public enum DigestEmailStatus {
    COLLECTING,
    PENDING,
    PROCESSING,
    SENT,
    DRY_RUN,
    FAILED_RETRYABLE,
    FAILED_PERMANENT,
    SKIPPED_PREFERENCE,
    SKIPPED_INELIGIBLE
}
