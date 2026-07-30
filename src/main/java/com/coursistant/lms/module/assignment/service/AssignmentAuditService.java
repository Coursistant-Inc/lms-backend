package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.AssignmentAuditLog;
import com.coursistant.lms.module.assignment.repository.AssignmentAuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Writes {@code assignment_audit_log} rows. Audit writes participate in the caller's
 * transaction so an audited change is never persisted without its trail.
 */
@Service
public class AssignmentAuditService {

    public static final String ASSIGNMENT_CREATED = "ASSIGNMENT_CREATED";
    public static final String ASSIGNMENT_UPDATED = "ASSIGNMENT_UPDATED";
    public static final String ASSIGNMENT_PUBLISHED = "ASSIGNMENT_PUBLISHED";
    public static final String ASSIGNMENT_UNPUBLISHED = "ASSIGNMENT_UNPUBLISHED";
    public static final String ASSIGNMENT_DELETED = "ASSIGNMENT_DELETED";
    public static final String DUE_DATE_SHORTENED = "DUE_DATE_SHORTENED";
    public static final String POINTS_CHANGED_AFTER_GRADING = "POINTS_CHANGED_AFTER_GRADING";
    public static final String ATTACHMENT_ADDED = "ATTACHMENT_ADDED";
    public static final String ATTACHMENT_DELETED = "ATTACHMENT_DELETED";
    public static final String RUBRIC_UPLOADED = "RUBRIC_UPLOADED";
    public static final String RUBRIC_REPLACED_AFTER_GRADING = "RUBRIC_REPLACED_AFTER_GRADING";
    public static final String RUBRIC_RESTORED_PREVIOUS = "RUBRIC_RESTORED_PREVIOUS";
    public static final String RUBRIC_RESTORED_AFTER_GRADING = "RUBRIC_RESTORED_AFTER_GRADING";
    public static final String SUBMISSION_CREATED = "SUBMISSION_CREATED";
    public static final String GRADE_UPSERTED = "GRADE_UPSERTED";
    public static final String GRADE_ANNOTATED_FILE_UPLOADED = "GRADE_ANNOTATED_FILE_UPLOADED";
    public static final String GRADE_CORRECTED_AFTER_RELEASE = "GRADE_CORRECTED_AFTER_RELEASE";
    public static final String GRADES_RELEASED = "GRADES_RELEASED";
    public static final String GRADES_RETRACTED = "GRADES_RETRACTED";

    private static final Logger log = LoggerFactory.getLogger(AssignmentAuditService.class);

    @Resource
    private AssignmentAuditLogMapper assignmentAuditLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Integer write(Integer courseId, Integer assignmentId, Integer actorUserId, String action, String detailJson) {
        AssignmentAuditLog auditLog = new AssignmentAuditLog();
        auditLog.setCourseId(courseId);
        auditLog.setAssignmentId(assignmentId);
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setDetailJson(detailJson);
        assignmentAuditLogMapper.insert(auditLog);
        return auditLog.getId();
    }

    public Integer write(Integer courseId, Integer assignmentId, Integer actorUserId, String action, Map<String, ?> detail) {
        return write(courseId, assignmentId, actorUserId, action, toJson(detail));
    }

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize assignment audit detail: {}", e.getMessage());
            return null;
        }
    }
}
