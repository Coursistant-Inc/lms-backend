package com.coursistant.lms.v2.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EditAssignmentRequest(
        @Nullable @Size(max = 63) String title,
        @Nullable @Size(max = 1000) String description,
        @Nullable @Size(max = 31) String type,
        @Nullable Instant dueTime,
        @Nullable @Valid AssignmentSettings settings,
        @Nullable Integer gradePublish
) {
    public boolean hasUpdates() {
        return title != null || description != null || type != null ||
                dueTime != null || settings != null || gradePublish != null;
    }

    public record AssignmentSettings(
            Boolean allowLateSubmission,
            @Min(-1) Integer allowedResubmissionCount
    ) {

    }
}
