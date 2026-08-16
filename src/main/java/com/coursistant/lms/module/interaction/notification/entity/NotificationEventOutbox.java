package com.coursistant.lms.module.interaction.notification.entity;

import java.time.LocalDateTime;

public class NotificationEventOutbox {

    private Long id;
    private String eventId;
    private Integer tenantId;
    private Integer courseId;
    private String notificationType;
    private String subjectType;
    private Integer subjectId;
    private String eventKey;
    private Integer actorUserId;
    private String message;
    private String deepLink;
    private LocalDateTime occurredAt;
    private String recipientMode;
    private String templateVarsJson;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime leaseUntil;
    private String claimToken;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public Integer getActorUserId() { return actorUserId; }
    public void setActorUserId(Integer actorUserId) { this.actorUserId = actorUserId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getRecipientMode() { return recipientMode; }
    public void setRecipientMode(String recipientMode) { this.recipientMode = recipientMode; }
    public String getTemplateVarsJson() { return templateVarsJson; }
    public void setTemplateVarsJson(String templateVarsJson) { this.templateVarsJson = templateVarsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public LocalDateTime getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(LocalDateTime leaseUntil) { this.leaseUntil = leaseUntil; }
    public String getClaimToken() { return claimToken; }
    public void setClaimToken(String claimToken) { this.claimToken = claimToken; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
