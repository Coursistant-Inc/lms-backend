package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatchQuizRequest {
    private String title;
    private String instructions;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private Integer timeLimitSeconds;
    private Integer attemptsAllowed;
    private String resultVisibility;
    private Integer expectedVersion;
}
