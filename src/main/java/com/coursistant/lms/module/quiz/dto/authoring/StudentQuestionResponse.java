package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StudentQuestionResponse {
    private Integer id;
    private Integer quizId;
    private String type;
    private String stem;
    private BigDecimal points;
    private Integer position;
    private List<StudentOptionResponse> options;
}
