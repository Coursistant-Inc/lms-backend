package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AssignmentGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer assignmentId;
    private Integer studentUserId;
    private Integer groupId;
    private Integer submissionVersionId;
    private Integer rubricVersionId;
    private BigDecimal score;
    private String feedbackHtml;
    private String annotatedObjectKey;
    private String annotatedOriginalName;
    private String annotatedContentType;
    private Long annotatedSizeBytes;
    private String status;
    private Integer enteredBy;
    private LocalDateTime enteredAt;
    private Integer editedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime releasedAt;
    private Boolean aiAssisted;
    private String aiProvenanceJson;

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

    public Integer getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Integer studentUserId) {
        this.studentUserId = studentUserId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getSubmissionVersionId() {
        return submissionVersionId;
    }

    public void setSubmissionVersionId(Integer submissionVersionId) {
        this.submissionVersionId = submissionVersionId;
    }

    public Integer getRubricVersionId() {
        return rubricVersionId;
    }

    public void setRubricVersionId(Integer rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getFeedbackHtml() {
        return feedbackHtml;
    }

    public void setFeedbackHtml(String feedbackHtml) {
        this.feedbackHtml = feedbackHtml;
    }

    public String getAnnotatedObjectKey() {
        return annotatedObjectKey;
    }

    public void setAnnotatedObjectKey(String annotatedObjectKey) {
        this.annotatedObjectKey = annotatedObjectKey;
    }

    public String getAnnotatedOriginalName() {
        return annotatedOriginalName;
    }

    public void setAnnotatedOriginalName(String annotatedOriginalName) {
        this.annotatedOriginalName = annotatedOriginalName;
    }

    public String getAnnotatedContentType() {
        return annotatedContentType;
    }

    public void setAnnotatedContentType(String annotatedContentType) {
        this.annotatedContentType = annotatedContentType;
    }

    public Long getAnnotatedSizeBytes() {
        return annotatedSizeBytes;
    }

    public void setAnnotatedSizeBytes(Long annotatedSizeBytes) {
        this.annotatedSizeBytes = annotatedSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(Integer enteredBy) {
        this.enteredBy = enteredBy;
    }

    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(LocalDateTime enteredAt) {
        this.enteredAt = enteredAt;
    }

    public Integer getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(Integer editedBy) {
        this.editedBy = editedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Boolean getAiAssisted() {
        return aiAssisted;
    }

    public void setAiAssisted(Boolean aiAssisted) {
        this.aiAssisted = aiAssisted;
    }

    public String getAiProvenanceJson() {
        return aiProvenanceJson;
    }

    public void setAiProvenanceJson(String aiProvenanceJson) {
        this.aiProvenanceJson = aiProvenanceJson;
    }
}
