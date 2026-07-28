package com.coursistant.lms.module.quiz.dto.attempt;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SavedAnswerResponse {
    private Integer questionId;
    private List<Integer> selectedOptionIds;
    private String textAnswer;
    private Integer revision;
    private LocalDateTime savedAt;
}
