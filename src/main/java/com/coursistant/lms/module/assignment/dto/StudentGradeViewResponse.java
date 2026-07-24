package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Student-visible grade block. Only populated when status is Released.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentGradeViewResponse {

    private BigDecimal score;
    private BigDecimal pointsEarned;
    private BigDecimal pointsPossible;
    private String feedbackHtml;
    private Boolean hasFeedback;
    private LocalDateTime releasedAt;
    private String gradeStatus;

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

    public Boolean getHasFeedback() {
        return hasFeedback;
    }

    public void setHasFeedback(Boolean hasFeedback) {
        this.hasFeedback = hasFeedback;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getGradeStatus() {
        return gradeStatus;
    }

    public void setGradeStatus(String gradeStatus) {
        this.gradeStatus = gradeStatus;
    }
}
