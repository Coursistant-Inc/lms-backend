package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OptionKeyInput", description = "Answer-key flip for an existing option")
public class OptionKeyInput {
    @Schema(description = "Existing option id", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer optionId;
    @Schema(description = "Whether this option is correct", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isCorrect;
}
