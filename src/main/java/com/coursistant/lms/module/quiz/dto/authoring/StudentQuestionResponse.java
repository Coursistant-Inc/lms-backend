package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(name = "StudentQuestionResponse",
        description = "Student-safe question view; never includes answer keys (no isCorrect / version)")
public class StudentQuestionResponse implements QuestionView {
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
    @Schema(description = "Student-safe options without isCorrect")
    private List<StudentOptionResponse> options;
}
