package com.coursistant.lms.module.course.group.dto;

import java.time.LocalDateTime;

public class MembershipResponse {
    private Integer groupId;
    private Integer userId;
    private String displayName;
    private LocalDateTime joinedAt;
    private String addedByType;
    private Integer addedByUserId;

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

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

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getAddedByType() {
        return addedByType;
    }

    public void setAddedByType(String addedByType) {
        this.addedByType = addedByType;
    }

    public Integer getAddedByUserId() {
        return addedByUserId;
    }

    public void setAddedByUserId(Integer addedByUserId) {
        this.addedByUserId = addedByUserId;
    }
}
