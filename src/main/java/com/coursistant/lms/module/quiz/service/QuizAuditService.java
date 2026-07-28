package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.entity.QuizAuditLog;
import com.coursistant.lms.module.quiz.repository.QuizAuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class QuizAuditService {

    @Resource
    private QuizAuditLogMapper quizAuditLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void log(Integer courseId, Integer quizId, Integer attemptId, Integer actorUserId,
                    String action, String reason, Map<String, Object> detail) {
        QuizAuditLog log = new QuizAuditLog();
        log.setCourseId(courseId);
        log.setQuizId(quizId);
        log.setAttemptId(attemptId);
        log.setActorUserId(actorUserId);
        log.setAction(action);
        log.setReason(reason);
        try {
            log.setDetailJson(objectMapper.writeValueAsString(detail == null ? Map.of() : detail));
        } catch (Exception e) {
            log.setDetailJson("{}");
        }
        log.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        quizAuditLogMapper.insert(log);
    }
}
