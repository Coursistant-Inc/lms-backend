package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Grade as seen by grading staff. Students never receive this shape directly; scores are only
 * exposed to a student once the grade is Released (see {@link MyGradeResponse}).
 */
@Schema(name = "GradeResponse", description = "Staff-facing grade record")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GradeResponse {

    private Integer id;
    private Integer assignmentId;
    private Integer studentUserId;
    private Integer groupId;
    private Integer submissionVersionId;
    private Integer rubricVersionId;
    private BigDecimal score;
    private BigDecimal pointsPossible;
    private String feedbackHtml;
    private String status;
    private Boolean hasAnnotatedFile;
    private String annotatedOriginalName;
    private String annotatedContentType;
    private Long annotatedSizeBytes;
    private String annotatedFileUrl;
    private Integer enteredBy;
    private Instant enteredAt;
    private Integer editedBy;
    private Instant updatedAt;
    private Instant releasedAt;
    private Boolean aiAssisted;

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

    public BigDecimal getPointsPossible() {
        return pointsPossible;
    }

    public void setPointsPossible(BigDecimal pointsPossible) {
        this.pointsPossible = pointsPossible;
    }

    public String getFeedbackHtml() {
        return feedbackHtml;
    }

    public void setFeedbackHtml(String feedbackHtml) {
        this.feedbackHtml = feedbackHtml;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getHasAnnotatedFile() {
        return hasAnnotatedFile;
    }

    public void setHasAnnotatedFile(Boolean hasAnnotatedFile) {
        this.hasAnnotatedFile = hasAnnotatedFile;
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

    public String getAnnotatedFileUrl() {
        return annotatedFileUrl;
    }

    public void setAnnotatedFileUrl(String annotatedFileUrl) {
        this.annotatedFileUrl = annotatedFileUrl;
    }

    public Integer getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(Integer enteredBy) {
        this.enteredBy = enteredBy;
    }

    public Instant getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(Instant enteredAt) {
        this.enteredAt = enteredAt;
    }

    public Integer getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(Integer editedBy) {
        this.editedBy = editedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Boolean getAiAssisted() {
        return aiAssisted;
    }

    public void setAiAssisted(Boolean aiAssisted) {
        this.aiAssisted = aiAssisted;
    }
}
