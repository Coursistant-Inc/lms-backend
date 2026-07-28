package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

@Data
public class OptionInput {
    private Integer id;
    private String label;
    private Boolean isCorrect;
    private Integer position;
}
