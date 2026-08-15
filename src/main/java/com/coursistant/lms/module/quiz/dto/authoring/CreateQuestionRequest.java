package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(name = "CreateQuestionRequest", description = "Create a quiz question with optional answer-key options")
public class CreateQuestionRequest {
    @Schema(description = "Question type", example = "SingleChoice", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"SingleChoice", "MultipleSelect", "TrueFalse", "ShortAnswer"})
    private String type;
    @Schema(description = "Question stem", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stem;
    @Schema(description = "Points (>= 0)", example = "2.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal points;
    @Schema(description = "Options with isCorrect; required for objective types, empty for ShortAnswer")
    private List<OptionInput> options;
}
