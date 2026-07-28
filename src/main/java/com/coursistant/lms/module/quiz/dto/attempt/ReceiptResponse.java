package com.coursistant.lms.module.quiz.dto.attempt;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReceiptResponse {
    private Integer attemptId;
    private String receiptId;
    private LocalDateTime submittedAt;
}
