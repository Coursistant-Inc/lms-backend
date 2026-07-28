package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QuizGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer quizId;
    private Integer userId;
    private Integer countedAttemptId;
    private String status;
    private Integer version;
    private LocalDateTime releasedAt;
    private Integer releasedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
