package com.coursistant.lms.module.quiz.dto.attempt;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class AttemptSummaryResponse {
    private Integer id;
    private Integer attemptNumber;
    private String status;
    private String closeReason;
    private LocalDateTime startedAt;
    private Instant submittedAt;
    private String receiptId;
}
