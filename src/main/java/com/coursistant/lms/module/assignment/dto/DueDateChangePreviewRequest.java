package com.coursistant.lms.module.assignment.dto;

import java.time.LocalDateTime;

/**
 * Dry-run payload for a due date change. Times are wall-clock in the course tenant timezone.
 */
public class DueDateChangePreviewRequest {

    private LocalDateTime dueAt;
    private LocalDateTime lateUntil;
    private Boolean clearLateUntil;

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public LocalDateTime getLateUntil() {
        return lateUntil;
    }

    public void setLateUntil(LocalDateTime lateUntil) {
        this.lateUntil = lateUntil;
    }

    public Boolean getClearLateUntil() {
        return clearLateUntil;
    }

    public void setClearLateUntil(Boolean clearLateUntil) {
        this.clearLateUntil = clearLateUntil;
    }
}
