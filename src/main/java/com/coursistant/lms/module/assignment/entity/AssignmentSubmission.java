package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Head row of a submission. Ownership is exclusive: an Individual assignment fills
 * {@code ownerUserId} and leaves {@code groupId} null, a Group assignment does the reverse
 * (enforced by {@code chk_assignment_submission_owner}).
 */
public class AssignmentSubmission implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer assignmentId;
    private Integer ownerUserId;
    private Integer groupId;
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

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
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
