package com.coursistant.lms.module.quiz.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "GradingSummaryResponse", description = "Instructor grading progress counters")
public class GradingSummaryResponse {
    @Schema(description = "Short-answer answers still pending manual grade", example = "4")
    private int pendingShortAnswerCount;
    @Schema(description = "Submitted attempts", example = "20")
    private int submittedAttemptCount;
    @Schema(description = "Users whose grades are released", example = "18")
    private int releasedUserCount;
    @Schema(description = "Submitted attempts with incomplete manual grading", example = "2")
    private int manualIncompleteAttemptCount;
}
