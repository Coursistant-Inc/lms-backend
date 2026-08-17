package com.coursistant.lms.module.interaction.notification.enums;

public enum RecipientMode {
    EXPLICIT,
    /** Leftover Phase 1 outbox rows only. New producers must not write this mode. */
    COURSE_ACTIVE_STUDENTS
}
