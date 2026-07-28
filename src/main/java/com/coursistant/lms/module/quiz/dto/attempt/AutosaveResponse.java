package com.coursistant.lms.module.quiz.dto.attempt;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AutosaveResponse {
    private Integer attemptId;
    private Integer questionId;
    private Integer revision;
    private LocalDateTime savedAtUtc;
    private LocalDateTime serverNowUtc;
    private LocalDateTime deadlineAtUtc;
}
