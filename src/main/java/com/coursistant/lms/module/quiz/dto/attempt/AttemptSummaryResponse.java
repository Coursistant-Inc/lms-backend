package com.coursistant.lms.module.quiz.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Schema(name = "AttemptSummaryResponse", description = "Slim attempt list row")
public class AttemptSummaryResponse {
    @Schema(description = "Attempt id", example = "50")
    private Integer id;
    @Schema(description = "1-based attempt number", example = "1")
    private Integer attemptNumber;
    @Schema(description = "Attempt status", example = "Submitted",
            allowableValues = {"InProgress", "Finalizing", "Submitted"})
    private String status;
    @Schema(description = "Close reason when submitted")
    private String closeReason;
    @Schema(description = "Start time (UTC local)")
    private LocalDateTime startedAt;
    @Schema(description = "Submit instant (UTC)")
    private Instant submittedAt;
    @Schema(description = "Opaque receipt id after submit")
    private String receiptId;
}
