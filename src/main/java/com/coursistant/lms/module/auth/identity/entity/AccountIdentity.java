package com.coursistant.lms.module.auth.identity.entity;

import java.time.LocalDateTime;

public class AccountIdentity {
    private Integer id;
    private String normalizedEmail;
    private String principalType;
    private Integer principalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public void setNormalizedEmail(String normalizedEmail) { this.normalizedEmail = normalizedEmail; }
    public String getPrincipalType() { return principalType; }
    public void setPrincipalType(String principalType) { this.principalType = principalType; }
    public Integer getPrincipalId() { return principalId; }
    public void setPrincipalId(Integer principalId) { this.principalId = principalId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
