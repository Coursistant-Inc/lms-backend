package com.coursistant.lms.module.quiz.dto.grading;

import lombok.Data;

@Data
public class GradingSummaryResponse {
    private int pendingShortAnswerCount;
    private int submittedAttemptCount;
    private int releasedUserCount;
    private int manualIncompleteAttemptCount;
}
