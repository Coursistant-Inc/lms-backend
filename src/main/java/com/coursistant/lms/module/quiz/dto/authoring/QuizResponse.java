package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class QuizResponse {
    private Integer id;
    private Integer courseId;
    private String title;
    private String instructions;
    private Instant opensAtUtc;
    private LocalDateTime opensAtLocal;
    private Instant closesAtUtc;
    private LocalDateTime closesAtLocal;
    private String timezone;
    private Integer timeLimitSeconds;
    private Integer attemptsAllowed;
    private String resultVisibility;
    private String state;
    private Integer version;
    private BigDecimal totalPoints;
    private Integer questionCount;
    private Boolean hasAttempts;
    private Instant createdAt;
    private Instant updatedAt;
}
