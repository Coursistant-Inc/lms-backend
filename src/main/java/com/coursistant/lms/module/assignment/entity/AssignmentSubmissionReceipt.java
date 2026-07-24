package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AssignmentSubmissionReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer submissionVersionId;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubmissionVersionId() {
        return submissionVersionId;
    }

    public void setSubmissionVersionId(Integer submissionVersionId) {
        this.submissionVersionId = submissionVersionId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
