package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateQuizRequest {
    private String title;
    private String instructions;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private Integer timeLimitSeconds;
    private Integer attemptsAllowed;
    private String resultVisibility;
}
