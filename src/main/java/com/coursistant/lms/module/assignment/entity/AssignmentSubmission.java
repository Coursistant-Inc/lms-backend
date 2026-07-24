package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AssignmentSubmission implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer assignmentId;
    private Integer ownerUserId;
    private Integer currentVersionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Integer ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Integer getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(Integer currentVersionId) {
        this.currentVersionId = currentVersionId;
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
