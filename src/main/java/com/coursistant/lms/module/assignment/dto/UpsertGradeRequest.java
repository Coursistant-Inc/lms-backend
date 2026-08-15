package com.coursistant.lms.module.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Upsert payload for a grade. {@code score} is always required: "Ungraded" is represented by
 * the absence of a grade row, never by a null score.
 */
@Schema(name = "UpsertGradeRequest", description = "Create or update a grade; score is always required")
public class UpsertGradeRequest {

    @Schema(description = "Numeric score (required; never null for Ungraded — omit the grade row instead)",
            example = "92.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal score;
    @Schema(description = "HTML feedback for the student (visible after release)", example = "<p>Solid work.</p>")
    private String feedbackHtml;
    @Schema(description = "Submission version this grade applies to", example = "3")
    private Integer submissionVersionId;
    @Schema(description = "Rubric version used while grading", example = "2")
    private Integer rubricVersionId;
    @Schema(description = "Whether AI assistance was used", example = "false")
    private Boolean aiAssisted;
    @Schema(description = "Opaque AI provenance JSON when aiAssisted is true")
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
