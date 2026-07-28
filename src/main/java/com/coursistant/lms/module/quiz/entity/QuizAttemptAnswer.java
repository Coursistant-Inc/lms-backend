package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuizAttemptAnswer implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer attemptId;
    private Integer questionId;
    private String selectedOptionIdsJson;
    private String textAnswer;
    private Integer revision;
    private LocalDateTime savedAt;
    private BigDecimal score;
    private Boolean pendingManual;
    private String feedback;
    private Integer gradedBy;
    private LocalDateTime gradedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
