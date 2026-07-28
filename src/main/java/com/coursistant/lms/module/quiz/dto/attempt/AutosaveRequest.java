package com.coursistant.lms.module.quiz.dto.attempt;

import lombok.Data;

import java.util.List;

@Data
public class AutosaveRequest {
    private List<Integer> selectedOptionIds;
    private String textAnswer;
}
