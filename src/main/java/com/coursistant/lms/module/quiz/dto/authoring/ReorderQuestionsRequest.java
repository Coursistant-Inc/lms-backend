package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "ReorderQuestionsRequest", description = "New question order; must include every question id exactly once")
public class ReorderQuestionsRequest {
    @Schema(description = "Ordered question ids", requiredMode = Schema.RequiredMode.REQUIRED, example = "[10, 12, 11]")
    private List<Integer> questionIds;
}
