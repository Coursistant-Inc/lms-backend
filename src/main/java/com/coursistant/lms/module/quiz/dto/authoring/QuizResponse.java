package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuizResponse {
    private Integer id;
    private Integer courseId;
    private String title;
    private String instructions;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private Integer timeLimitSeconds;
    private Integer attemptsAllowed;
    private String resultVisibility;
    private String state;
    private Integer version;
    private BigDecimal totalPoints;
    private Integer questionCount;
    private Boolean hasAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
