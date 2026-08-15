package com.coursistant.lms.module.quiz.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "AutosaveRequest", description = "Save one question answer during an in-progress attempt")
public class AutosaveRequest {
    @Schema(description = "Selected option ids for objective questions")
    private List<Integer> selectedOptionIds;
    @Schema(description = "Free-text answer for ShortAnswer")
    private String textAnswer;
}
