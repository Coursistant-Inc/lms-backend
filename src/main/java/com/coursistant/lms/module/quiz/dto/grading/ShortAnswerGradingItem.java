package com.coursistant.lms.module.quiz.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "ShortAnswerGradingItem", description = "One short-answer row for manual grading")
public class ShortAnswerGradingItem {
    @Schema(description = "Attempt id", example = "50")
    private Integer attemptId;
    @Schema(description = "Student user id", example = "21")
    private Integer userId;
    @Schema(description = "Question id", example = "12")
    private Integer questionId;
    @Schema(description = "Student text answer")
    private String textAnswer;
    @Schema(description = "Current score if graded")
    private BigDecimal score;
    @Schema(description = "True when still awaiting manual grade")
    private Boolean pendingManual;
    @Schema(description = "Grader feedback")
    private String feedback;
}
