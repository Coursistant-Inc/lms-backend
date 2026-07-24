package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AssignmentSubmissionVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer submissionId;
    private Integer assignmentId;
    private Integer ownerUserId;
    private Integer versionNo;
    private LocalDateTime submittedAt;
    private Boolean usedGraceBuffer;
    private LocalDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getUsedGraceBuffer() {
        return usedGraceBuffer;
    }

    public void setUsedGraceBuffer(Boolean usedGraceBuffer) {
        this.usedGraceBuffer = usedGraceBuffer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
