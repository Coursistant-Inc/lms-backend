package com.coursistant.lms.module.course.announcement.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CourseAnnouncementRead implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer announcementId;
    private Integer userId;
    private LocalDateTime readAt;

    public Integer getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(Integer announcementId) { this.announcementId = announcementId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
