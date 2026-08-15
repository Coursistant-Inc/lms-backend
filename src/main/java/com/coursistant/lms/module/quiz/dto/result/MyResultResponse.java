package com.coursistant.lms.module.quiz.dto.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(name = "MyResultResponse", description = "Student-facing quiz result / counted attempt summary")
public class MyResultResponse {
    @Schema(description = "Quiz id", example = "3")
    private Integer quizId;
    @Schema(description = "Attempt id that counts toward grade")
    private Integer countedAttemptId;
    @Schema(description = "Grade status", example = "Released", allowableValues = {"Entered", "Released"})
    private String gradeStatus;
    @Schema(description = "Close reason of counted attempt")
    private String closeReason;
    @Schema(description = "Receipt id of counted attempt")
    private String receiptId;
    @Schema(description = "Auto-scored points")
    private BigDecimal autoScore;
    @Schema(description = "Manual points")
    private BigDecimal manualScore;
    @Schema(description = "Total points")
    private BigDecimal totalScore;
    @Schema(description = "True when short answers still need grading")
    private Boolean manualGradingPending;
    @Schema(description = "Whether correct answers are included in questions[]")
    private Boolean showCorrectAnswers;
    @Schema(description = "Per-question breakdown when visible")
    private List<QuestionResultItem> questions;
}
