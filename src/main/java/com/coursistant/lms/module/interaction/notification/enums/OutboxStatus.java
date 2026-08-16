package com.coursistant.lms.module.interaction.notification.enums;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    FAILED_RETRYABLE,
    DONE,
    FAILED_PERMANENT
}
