package com.coursistant.lms.module.interaction.notification.dto;

import java.time.LocalDate;

public class DigestRecipientKey {

    private Integer tenantId;
    private Integer recipientUserId;

    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
    public Integer getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Integer recipientUserId) { this.recipientUserId = recipientUserId; }

    public record Pair(Integer tenantId, Integer recipientUserId, LocalDate digestDate) {
    }
}
