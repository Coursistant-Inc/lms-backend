package com.coursistant.lms.module.course.announcement.dto;

import java.time.LocalDateTime;

public class AnnouncementResponse {
    private Integer id;
    private Integer courseId;
    private String title;
    private String body;
    private Integer authorUserId;
    private String authorName;
    private LocalDateTime postedAt;
    private LocalDateTime editedAt;
    private Boolean read;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Integer getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(Integer authorUserId) { this.authorUserId = authorUserId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
}
