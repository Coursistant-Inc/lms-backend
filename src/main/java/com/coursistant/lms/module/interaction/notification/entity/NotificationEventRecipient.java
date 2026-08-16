package com.coursistant.lms.module.interaction.notification.entity;

public class NotificationEventRecipient {

    private Long id;
    private Long outboxId;
    private Integer recipientUserId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOutboxId() { return outboxId; }
    public void setOutboxId(Long outboxId) { this.outboxId = outboxId; }
    public Integer getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Integer recipientUserId) { this.recipientUserId = recipientUserId; }
}
