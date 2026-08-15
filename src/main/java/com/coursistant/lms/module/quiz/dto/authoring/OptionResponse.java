package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OptionResponse", description = "Instructor option including answer-key flag")
public class OptionResponse {
    @Schema(description = "Option id", example = "101")
    private Integer id;
    @Schema(description = "Option label / text")
    private String label;
    @Schema(description = "Whether this option is correct (answer key; instructor only)", example = "true")
    private Boolean isCorrect;
    @Schema(description = "Display position", example = "1")
    private Integer position;
}
