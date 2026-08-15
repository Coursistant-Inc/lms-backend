package com.coursistant.lms.module.quiz.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(name = "ReceiptResponse", description = "Submission receipt for a finalized attempt")
public class ReceiptResponse {
    @Schema(description = "Attempt id", example = "50")
    private Integer attemptId;
    @Schema(description = "Opaque receipt id")
    private String receiptId;
    @Schema(description = "Submit instant (UTC)")
    private Instant submittedAt;
}
