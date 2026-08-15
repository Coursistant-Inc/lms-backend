package com.coursistant.lms.module.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Create payload. {@code dueAt}/{@code lateUntil} are wall-clock times in the course tenant
 * timezone and are converted to UTC before persisting.
 * Group assignments require {@code submissionType=Group} and a {@code groupSetId}.
 */
@Schema(name = "CreateAssignmentRequest", description = "Create assignment payload")
public class CreateAssignmentRequest {

    @Schema(description = "Assignment title", example = "Week 3 Lab Report", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(description = "Rich-text or plain description", example = "Submit your lab write-up as PDF.")
    private String description;
    @Schema(description = "Maximum points", example = "100")
    private BigDecimal pointsPossible;
    @Schema(description = "Due datetime (wall-clock in course tenant timezone)", example = "2026-09-15T23:59:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime dueAt;
    @Schema(description = "Late window end (wall-clock in course tenant timezone); omit for no late window",
            example = "2026-09-16T23:59:00")
    private LocalDateTime lateUntil;
    @Schema(description = "Allowed file extensions/types", example = "[\"pdf\",\"docx\"]")
    private List<String> allowedFileTypes;
    @Schema(description = "Max size per file in bytes", example = "10485760")
    private Long maxFileSizeBytes;
    @Schema(description = "Max number of files per submission", example = "3")
    private Integer maxFileCount;
    @Schema(description = "Individual or Group", example = "Individual",
            allowableValues = {"Individual", "Group"})
    private String submissionType;
    @Schema(description = "Required when submissionType=Group", example = "7")
    private Integer groupSetId;

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

    public List<String> getAllowedFileTypes() {
        return allowedFileTypes;
    }

    public void setAllowedFileTypes(List<String> allowedFileTypes) {
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
}
