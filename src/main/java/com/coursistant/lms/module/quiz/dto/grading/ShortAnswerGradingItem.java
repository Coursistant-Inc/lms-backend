package com.coursistant.lms.module.quiz.dto.grading;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShortAnswerGradingItem {
    private Integer attemptId;
    private Integer userId;
    private Integer questionId;
    private String textAnswer;
    private BigDecimal score;
    private Boolean pendingManual;
    private String feedback;
}
