package com.coursistant.lms.module.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Dry-run payload for a due date change. Times are wall-clock in the course tenant timezone.
 */
@Schema(name = "DueDateChangePreviewRequest", description = "Dry-run due date change preview")
public class DueDateChangePreviewRequest {

    @Schema(description = "Proposed due datetime (wall-clock in course tenant timezone)", example = "2026-09-20T23:59:00")
    private LocalDateTime dueAt;
    @Schema(description = "Proposed late-until datetime", example = "2026-09-21T23:59:00")
    private LocalDateTime lateUntil;
    @Schema(description = "When true, preview clearing the late window", example = "false")
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
