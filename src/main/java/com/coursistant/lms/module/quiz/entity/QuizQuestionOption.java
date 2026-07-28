package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuizQuestionOption implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer questionId;
    private String label;
    private Boolean isCorrect;
    private Integer position;
}
