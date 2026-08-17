package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.enums.NotificationType;

/**
 * Maps each notification type to its business audience and email channel.
 * Persistence is always {@code RecipientMode.EXPLICIT} for new producers;
 * {@code COURSE_ACTIVE_STUDENTS} exists only as a Relay-time fallback for
 * leftover Phase 1 outbox rows.
 */
public final class NotificationPolicy {

    public enum EmailMode {
        IMMEDIATE,
        DIGEST
    }

    /**
     * Who should receive the event. Producers for course-wide types resolve this
     * via {@link NotificationRecipientResolver#resolveForType}.
     */
    public enum Audience {
        ACTIVE_STUDENTS,
        ALL_ACTIVE_COURSE_MEMBERS,
        PROVIDED_RECIPIENTS
    }

    public record Mapping(Audience audience, EmailMode emailMode) {
    }

    private NotificationPolicy() {
    }

    public static Mapping forType(NotificationType type) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        return switch (type) {
            case ANNOUNCEMENT_POSTED, COURSE_EVENT_CREATED ->
                    new Mapping(Audience.ALL_ACTIVE_COURSE_MEMBERS, EmailMode.DIGEST);
            case ASSIGNMENT_PUBLISHED, WEEK_PUBLISHED, ASSIGNMENT_SCHEDULE_CHANGED,
                 QUIZ_PUBLISHED, QUIZ_SCHEDULE_CHANGED, QUIZ_TIME_LIMIT_CHANGED ->
                    new Mapping(Audience.ACTIVE_STUDENTS, EmailMode.DIGEST);
            case GROUP_MEMBER_ADDED, GROUP_MEMBER_REMOVED, GROUP_MEMBER_MOVED ->
                    new Mapping(Audience.PROVIDED_RECIPIENTS, EmailMode.DIGEST);
            case ASSIGNMENT_SUBMISSION_RECEIVED, ASSIGNMENT_GRADE_RELEASED, ASSIGNMENT_GRADE_CORRECTED,
                 QUIZ_GRADE_RELEASED, QUIZ_GRADE_CORRECTED ->
                    new Mapping(Audience.PROVIDED_RECIPIENTS, EmailMode.IMMEDIATE);
        };
    }
}
