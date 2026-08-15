package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "PatchAnswerKeyRequest", description = "Patch answer keys and optionally trigger regrade")
public class PatchAnswerKeyRequest {
    @Schema(description = "Option id → isCorrect pairs", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OptionKeyInput> options;
    @Schema(description = "Reason for key change (required when attempts exist / regrade)", example = "Typo in key")
    private String reason;
    @Schema(description = "Expected question version", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedVersion;
}
