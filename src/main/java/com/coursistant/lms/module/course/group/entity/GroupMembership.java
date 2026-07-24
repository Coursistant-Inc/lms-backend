package com.coursistant.lms.module.course.group.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GroupMembership implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ADDED_BY_SELF = "Self";
    public static final String ADDED_BY_STAFF = "Staff";

    private Integer id;
    private Integer groupId;
    private Integer groupSetId;
    private Integer courseId;
    private Integer userId;
    private LocalDateTime joinedAt;
    private String addedByType;
    private Integer addedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getGroupSetId() {
        return groupSetId;
    }

    public void setGroupSetId(Integer groupSetId) {
        this.groupSetId = groupSetId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
