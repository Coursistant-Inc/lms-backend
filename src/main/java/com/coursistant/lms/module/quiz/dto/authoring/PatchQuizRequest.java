package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "PatchQuizRequest", description = "Partial quiz update; times are wall-clock in course timezone")
public class PatchQuizRequest {
    @Schema(description = "Title")
    private String title;
    @Schema(description = "Instructions")
    private String instructions;
    @Schema(description = "Window open wall-clock (course timezone)")
    private LocalDateTime opensAt;
    @Schema(description = "Window close wall-clock (course timezone)")
    private LocalDateTime closesAt;
    @Schema(description = "Per-attempt time limit in seconds")
    private Integer timeLimitSeconds;
    @Schema(description = "Max attempts allowed (>= 1)")
    private Integer attemptsAllowed;
    @Schema(description = "Result visibility policy",
            allowableValues = {"AfterRelease", "InstantAutoScore"})
    private String resultVisibility;
    @Schema(description = "Expected version for optimistic concurrency", example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedVersion;
}
