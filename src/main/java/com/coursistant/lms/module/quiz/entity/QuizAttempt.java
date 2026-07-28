package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuizAttempt implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer quizId;
    private Integer userId;
    private Integer attemptNumber;
    private String status;
    private String closeReason;
    private String receiptId;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime submittedAt;
    private BigDecimal autoScore;
    private BigDecimal manualScore;
    private BigDecimal totalScore;
    private Boolean manualGradingComplete;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
