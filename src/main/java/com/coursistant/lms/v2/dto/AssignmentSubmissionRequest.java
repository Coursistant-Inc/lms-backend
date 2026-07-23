package com.coursistant.lms.v2.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

public record AssignmentSubmissionRequest(
    @Nullable @Size(max = 1000) String submissionContent
) {
    public boolean hasUpdates() {
        return submissionContent != null;
    }
}
