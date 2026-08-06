package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.entity.CourseAuditLog;
import com.coursistant.lms.module.course.course.repository.CourseAuditLogMapper;
import com.coursistant.lms.shared.security.ActorContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes {@code course_audit_log}. Prefer calling inside the business transaction.
 * Never log passwords, tokens, cookies, or binary secrets in JSON payloads.
 */
@Service
public class CourseAuditService {

    private static final Logger log = LoggerFactory.getLogger(CourseAuditService.class);

    @Resource
    private CourseAuditLogMapper courseAuditLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Long write(ActorContext actor, Integer courseId, Integer tenantId, String action,
                      String targetType, Integer targetId, Object before, Object after, String requestId) {
        CourseAuditLog row = new CourseAuditLog();
        row.setCourseId(courseId);
        row.setTenantId(tenantId);
        row.setActorType(actor.getActorType());
        row.setActorId(actor.getActorId());
        row.setActorRole(actor.getRole());
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setBeforeJson(toJson(before));
        row.setAfterJson(toJson(after));
        row.setRequestId(requestId);
        courseAuditLogMapper.insert(row);
        return row.getId();
    }

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize course audit json: {}", e.getMessage());
            return null;
        }
    }
}
