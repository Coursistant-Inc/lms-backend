package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;

public final class NotificationPolicy {

    public enum EmailMode {
        IMMEDIATE,
        DIGEST
    }

    public record Mapping(RecipientMode recipientMode, EmailMode emailMode) {
    }

    private NotificationPolicy() {
    }

    public static Mapping forType(NotificationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        return switch (type) {
            case ANNOUNCEMENT_POSTED, ASSIGNMENT_PUBLISHED ->
                    new Mapping(RecipientMode.COURSE_ACTIVE_STUDENTS, EmailMode.DIGEST);
            case ASSIGNMENT_SUBMISSION_RECEIVED, ASSIGNMENT_GRADE_RELEASED, ASSIGNMENT_GRADE_CORRECTED,
                 QUIZ_GRADE_RELEASED, QUIZ_GRADE_CORRECTED ->
                    new Mapping(RecipientMode.EXPLICIT, EmailMode.IMMEDIATE);
        };
    }
}
