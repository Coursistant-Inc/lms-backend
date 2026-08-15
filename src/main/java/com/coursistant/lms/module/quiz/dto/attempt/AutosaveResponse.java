package com.coursistant.lms.module.quiz.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(name = "AutosaveResponse", description = "Autosave acknowledgement with server clocks")
public class AutosaveResponse {
    @Schema(description = "Attempt id", example = "50")
    private Integer attemptId;
    @Schema(description = "Question id", example = "10")
    private Integer questionId;
    @Schema(description = "Answer revision counter", example = "3")
    private Integer revision;
    @Schema(description = "Saved at (UTC local)")
    private LocalDateTime savedAtUtc;
    @Schema(description = "Server now (UTC local)")
    private LocalDateTime serverNowUtc;
    @Schema(description = "Attempt deadline (UTC local)")
    private LocalDateTime deadlineAtUtc;
}
