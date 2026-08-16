package com.coursistant.lms.module.interaction.notification.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotificationDigestEmail {

    private Long id;
    private Integer tenantId;
    private Integer recipientUserId;
    private LocalDate digestDate;
    private String status;
    private Integer itemCount;
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
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
    public Integer getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Integer recipientUserId) { this.recipientUserId = recipientUserId; }
    public LocalDate getDigestDate() { return digestDate; }
    public void setDigestDate(LocalDate digestDate) { this.digestDate = digestDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
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
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
