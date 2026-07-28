package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuizQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer quizId;
    private String type;
    private String stem;
    private BigDecimal points;
    private Integer position;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
