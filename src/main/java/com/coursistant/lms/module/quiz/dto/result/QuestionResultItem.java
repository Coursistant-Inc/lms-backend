package com.coursistant.lms.module.quiz.dto.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(name = "QuestionResultItem", description = "Per-question result row; correctOptionIds only when showCorrectAnswers")
public class QuestionResultItem {
    @Schema(description = "Question id", example = "10")
    private Integer questionId;
    @Schema(description = "Question type", example = "SingleChoice")
    private String type;
    @Schema(description = "Max points", example = "2.0")
    private BigDecimal points;
    @Schema(description = "Points awarded", example = "2.0")
    private BigDecimal score;
    @Schema(description = "Student selected option ids")
    private List<Integer> selectedOptionIds;
    @Schema(description = "Student short-answer text")
    private String textAnswer;
    @Schema(description = "Correct option ids when policy/visibility allows")
    private List<Integer> correctOptionIds;
}
