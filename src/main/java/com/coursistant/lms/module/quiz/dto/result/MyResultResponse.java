package com.coursistant.lms.module.quiz.dto.result;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MyResultResponse {
    private Integer quizId;
    private Integer countedAttemptId;
    private String gradeStatus;
    private String closeReason;
    private String receiptId;
    private BigDecimal autoScore;
    private BigDecimal manualScore;
    private BigDecimal totalScore;
    private Boolean manualGradingPending;
    private Boolean showCorrectAnswers;
    private List<QuestionResultItem> questions;
}
