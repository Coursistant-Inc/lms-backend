package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OptionInput", description = "Option payload for create/patch question")
public class OptionInput {
    @Schema(description = "Existing option id when updating; omit for new")
    private Integer id;
    @Schema(description = "Option label", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;
    @Schema(description = "Whether this option is correct", example = "true")
    private Boolean isCorrect;
    @Schema(description = "Display position", example = "1")
    private Integer position;
}
