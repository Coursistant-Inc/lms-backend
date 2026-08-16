package com.coursistant.lms.module.interaction.notification.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotificationDelivery {

    private Long id;
    private String eventId;
    private Integer tenantId;
    private Integer recipientUserId;
    private Integer courseId;
    private String notificationType;
    private String subjectType;
    private Integer subjectId;
    private String eventKey;
    private String channel;
    private String status;
    private String message;
    private String deepLink;
    private String templateVarsJson;
    private LocalDateTime occurredAt;
    private LocalDate digestDate;
    private Long digestEmailId;
    private Integer attemptCount;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime leaseUntil;
    private String claimToken;
    private LocalDateTime sendAttemptedAt;
    private Integer unknownOutcomeCount;
    private String failureCategory;
    private String lastError;
    private String providerMessageId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
    public Integer getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Integer recipientUserId) { this.recipientUserId = recipientUserId; }
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
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public String getTemplateVarsJson() { return templateVarsJson; }
    public void setTemplateVarsJson(String templateVarsJson) { this.templateVarsJson = templateVarsJson; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public LocalDate getDigestDate() { return digestDate; }
    public void setDigestDate(LocalDate digestDate) { this.digestDate = digestDate; }
    public Long getDigestEmailId() { return digestEmailId; }
    public void setDigestEmailId(Long digestEmailId) { this.digestEmailId = digestEmailId; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public LocalDateTime getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(LocalDateTime leaseUntil) { this.leaseUntil = leaseUntil; }
    public String getClaimToken() { return claimToken; }
    public void setClaimToken(String claimToken) { this.claimToken = claimToken; }
    public LocalDateTime getSendAttemptedAt() { return sendAttemptedAt; }
    public void setSendAttemptedAt(LocalDateTime sendAttemptedAt) { this.sendAttemptedAt = sendAttemptedAt; }
    public Integer getUnknownOutcomeCount() { return unknownOutcomeCount; }
    public void setUnknownOutcomeCount(Integer unknownOutcomeCount) { this.unknownOutcomeCount = unknownOutcomeCount; }
    public String getFailureCategory() { return failureCategory; }
    public void setFailureCategory(String failureCategory) { this.failureCategory = failureCategory; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
