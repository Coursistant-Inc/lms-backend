package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(name = "PatchQuestionRequest", description = "Partial question update (stem/points/options)")
public class PatchQuestionRequest {
    @Schema(description = "Question stem")
    private String stem;
    @Schema(description = "Points (>= 0)")
    private BigDecimal points;
    @Schema(description = "Replacement options including isCorrect when provided")
    private List<OptionInput> options;
    @Schema(description = "Expected version for optimistic concurrency", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedVersion;
}
