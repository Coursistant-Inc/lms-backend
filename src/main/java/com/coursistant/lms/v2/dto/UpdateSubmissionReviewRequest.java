package com.coursistant.lms.v2.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

public record UpdateSubmissionReviewRequest(
        @Nullable Integer grade,
        @Nullable @Size(max = 1000) String teacherComment
) {
    public boolean hasUpdate() {
        return grade != null || teacherComment != null;
    }
}
