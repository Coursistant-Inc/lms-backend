package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(name = "QuestionResponse", description = "Instructor question view including answer keys")
public class QuestionResponse implements QuestionView {
    @Schema(description = "Question id", example = "10")
    private Integer id;
    @Schema(description = "Parent quiz id", example = "3")
    private Integer quizId;
    @Schema(description = "Question type", example = "SingleChoice",
            allowableValues = {"SingleChoice", "MultipleSelect", "TrueFalse", "ShortAnswer"})
    private String type;
    @Schema(description = "Question stem / prompt")
    private String stem;
    @Schema(description = "Points for this question", example = "2.0")
    private BigDecimal points;
    @Schema(description = "1-based position in quiz", example = "1")
    private Integer position;
    @Schema(description = "Optimistic concurrency version", example = "1")
    private Integer version;
    @Schema(description = "Options including isCorrect answer-key flags")
    private List<OptionResponse> options;
}
