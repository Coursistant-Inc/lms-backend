package com.coursistant.lms.module.quiz.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QuizAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    private Integer quizId;
    private Integer attemptId;
    private Integer actorUserId;
    private String action;
    private String reason;
    private String detailJson;
    private LocalDateTime createdAt;
}
