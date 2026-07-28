package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.util.List;

@Data
public class ReorderQuestionsRequest {
    private List<Integer> questionIds;
}
