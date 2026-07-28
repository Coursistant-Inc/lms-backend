package com.coursistant.lms.module.quiz.dto.result;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionResultItem {
    private Integer questionId;
    private String type;
    private BigDecimal points;
    private BigDecimal score;
    private List<Integer> selectedOptionIds;
    private String textAnswer;
    private List<Integer> correctOptionIds;
}
