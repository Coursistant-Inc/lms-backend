package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "StudentOptionResponse", description = "Student-safe option without answer key")
public class StudentOptionResponse {
    @Schema(description = "Option id", example = "101")
    private Integer id;
    @Schema(description = "Option label / text")
    private String label;
    @Schema(description = "Display position", example = "1")
    private Integer position;
}
