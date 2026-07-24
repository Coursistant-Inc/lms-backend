package com.coursistant.lms.module.course.announcement.dto;

public class UpdateAnnouncementRequest {
    private String title;
    private String body;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
