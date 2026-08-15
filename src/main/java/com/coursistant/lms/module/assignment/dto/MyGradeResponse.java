package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A student's own view of one assignment's grade. Score, feedback, and the annotated file are
 * only populated once the grade status is {@code Released}; before that {@code released=false}
 * and the score fields stay absent.
 */
@Schema(name = "MyGradeResponse", description = "Student view of one assignment grade")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyGradeResponse {

    private Integer assignmentId;
    private String assignmentTitle;
    private String title;
    private String itemType = "Individual";
    private BigDecimal pointsPossible;
    private Instant dueAtUtc;
    private LocalDateTime dueAtLocal;
    private String timezone;
    private String submissionStatus;
    private Instant submittedAt;
    private Integer versionNo;
    private boolean released;
    private String gradeDisplay;
    private BigDecimal score;
    private BigDecimal pointsEarned;
    private String feedbackHtml;
    private Boolean hasFeedback;
    private Instant releasedAt;
    private Boolean hasAnnotatedFile;
    private String annotatedOriginalName;
    private String annotatedFileUrl;

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
        this.title = assignmentTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.assignmentTitle = title;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public BigDecimal getPointsPossible() {
        return pointsPossible;
    }

    public void setPointsPossible(BigDecimal pointsPossible) {
        this.pointsPossible = pointsPossible;
    }

    public Instant getDueAtUtc() {
        return dueAtUtc;
    }

    public void setDueAtUtc(Instant dueAtUtc) {
        this.dueAtUtc = dueAtUtc;
    }

    public LocalDateTime getDueAtLocal() {
        return dueAtLocal;
    }

    public void setDueAtLocal(LocalDateTime dueAtLocal) {
        this.dueAtLocal = dueAtLocal;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public String getGradeDisplay() {
        return gradeDisplay;
    }

    public void setGradeDisplay(String gradeDisplay) {
        this.gradeDisplay = gradeDisplay;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(BigDecimal pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public String getFeedbackHtml() {
        return feedbackHtml;
    }

    public void setFeedbackHtml(String feedbackHtml) {
        this.feedbackHtml = feedbackHtml;
    }

    public Boolean getHasFeedback() {
        return hasFeedback;
    }

    public void setHasFeedback(Boolean hasFeedback) {
        this.hasFeedback = hasFeedback;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
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

    public String getAnnotatedFileUrl() {
        return annotatedFileUrl;
    }

    public void setAnnotatedFileUrl(String annotatedFileUrl) {
        this.annotatedFileUrl = annotatedFileUrl;
    }
}
