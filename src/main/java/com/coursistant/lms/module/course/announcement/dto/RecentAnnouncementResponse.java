package com.coursistant.lms.module.course.announcement.dto;

import java.time.LocalDateTime;

/** Dashboard Recent Announcements slim item. */
public class RecentAnnouncementResponse {

    private Integer courseId;
    private Integer id;
    private String courseCode;
    private String title;
    private LocalDateTime postedAt;
    private Boolean unread;

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
    public Boolean getUnread() { return unread; }
    public void setUnread(Boolean unread) { this.unread = unread; }
}
