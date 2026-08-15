package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "CreateQuizRequest", description = "Create a draft quiz; times are wall-clock in course timezone")
public class CreateQuizRequest {
    @Schema(description = "Title", example = "Week 1 Quiz", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(description = "Instructions shown to students")
    private String instructions;
    @Schema(description = "Window open wall-clock (course timezone)", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime opensAt;
    @Schema(description = "Window close wall-clock (course timezone)", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime closesAt;
    @Schema(description = "Per-attempt time limit in seconds; omit or null for unlimited", example = "3600")
    private Integer timeLimitSeconds;
    @Schema(description = "Max attempts allowed (>= 1)", example = "1")
    private Integer attemptsAllowed;
    @Schema(description = "Result visibility policy", example = "AfterRelease",
            allowableValues = {"AfterRelease", "InstantAutoScore"})
    private String resultVisibility;
}
