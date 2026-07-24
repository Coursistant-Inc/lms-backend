package com.coursistant.lms.module.course.announcement.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String EVENT_ANNOUNCEMENT_POSTED = "ANNOUNCEMENT_POSTED";

    private Integer id;
    private Integer recipientUserId;
    private Integer courseId;
    private String eventType;
    private Integer refId;
    private String title;
    private String deepLink;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Integer recipientUserId) { this.recipientUserId = recipientUserId; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getRefId() { return refId; }
    public void setRefId(Integer refId) { this.refId = refId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
