package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuizScoreAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer quizId;
    private Integer attemptId;
    private Integer questionId;
    private Integer actorUserId;
    private String reason;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private LocalDateTime createdAt;
}
