package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    private String title;
    private String description;
    private BigDecimal pointsPossible;
    private LocalDateTime dueAt;
    private LocalDateTime lateUntil;
    private String submissionType;
    private Integer groupSetId;
    private String allowedFileTypes;
    private Long maxFileSizeBytes;
    private Integer maxFileCount;
    private String state;
    private Integer currentRubricVersionId;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPointsPossible() {
        return pointsPossible;
    }

    public void setPointsPossible(BigDecimal pointsPossible) {
        this.pointsPossible = pointsPossible;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public LocalDateTime getLateUntil() {
        return lateUntil;
    }

    public void setLateUntil(LocalDateTime lateUntil) {
        this.lateUntil = lateUntil;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public Integer getGroupSetId() {
        return groupSetId;
    }

    public void setGroupSetId(Integer groupSetId) {
        this.groupSetId = groupSetId;
    }

    public String getAllowedFileTypes() {
        return allowedFileTypes;
    }

    public void setAllowedFileTypes(String allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
    }

    public Long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public Integer getMaxFileCount() {
        return maxFileCount;
    }

    public void setMaxFileCount(Integer maxFileCount) {
        this.maxFileCount = maxFileCount;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getCurrentRubricVersionId() {
        return currentRubricVersionId;
    }

    public void setCurrentRubricVersionId(Integer currentRubricVersionId) {
        this.currentRubricVersionId = currentRubricVersionId;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
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
