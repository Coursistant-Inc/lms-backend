package com.coursistant.lms.module.quiz.dto.grading;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeAnswerRequest {
    private BigDecimal score;
    private String feedback;
    private String reason;
}
