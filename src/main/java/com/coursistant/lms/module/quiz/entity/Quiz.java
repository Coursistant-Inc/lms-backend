package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Quiz implements Serializable {
    private static final long serialVersionUID = 1L;

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
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
