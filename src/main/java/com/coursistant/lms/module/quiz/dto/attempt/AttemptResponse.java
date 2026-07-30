package com.coursistant.lms.module.quiz.dto.attempt;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttemptResponse {
    private Integer id;
    private Integer quizId;
    private Integer userId;
    private Integer attemptNumber;
    private String status;
    private String closeReason;
    private String receiptId;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private Instant submittedAt;
    private LocalDateTime serverNowUtc;
    private BigDecimal autoScore;
    private BigDecimal manualScore;
    private BigDecimal totalScore;
    private Boolean manualGradingComplete;
    private List<SavedAnswerResponse> answers;
}
