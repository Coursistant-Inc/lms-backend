package com.coursistant.lms.module.assignment.dto;

import java.math.BigDecimal;

/**
 * Upsert payload for a grade. {@code score} is always required: "Ungraded" is represented by
 * the absence of a grade row, never by a null score.
 */
public class UpsertGradeRequest {

    private BigDecimal score;
    private String feedbackHtml;
    private Integer submissionVersionId;
    private Integer rubricVersionId;
    private Boolean aiAssisted;
    private String aiProvenanceJson;

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
