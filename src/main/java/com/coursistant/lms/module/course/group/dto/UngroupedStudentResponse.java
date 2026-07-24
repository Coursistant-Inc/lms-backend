package com.coursistant.lms.module.course.group.dto;

public class UngroupedStudentResponse {
    private Integer userId;
    private String displayName;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
