package com.coursistant.lms.module.course.announcement.dto;

import java.time.LocalDateTime;

public class NotificationResponse {
    private Integer id;
    private String eventType;
    private Integer courseId;
    private String courseCode;
    private Integer refId;
    private String title;
    private String deepLink;
    private LocalDateTime createdAt;
    private Boolean read;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public Integer getRefId() { return refId; }
    public void setRefId(Integer refId) { this.refId = refId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
}
