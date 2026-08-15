package com.coursistant.lms.module.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Partial update payload. Only non-null fields are applied, except {@code clearLateUntil}
 * which explicitly removes the late window.
 */
@Schema(name = "PatchAssignmentRequest", description = "Partial assignment update; null fields are ignored")
public class PatchAssignmentRequest {

    @Schema(description = "New title", example = "Week 3 Lab Report (Revised)")
    private String title;
    @Schema(description = "New description")
    private String description;
    @Schema(description = "New points possible", example = "100")
    private BigDecimal pointsPossible;
    @Schema(description = "New due datetime (wall-clock in course tenant timezone)", example = "2026-09-20T23:59:00")
    private LocalDateTime dueAt;
    @Schema(description = "New late-until datetime; use clearLateUntil to remove", example = "2026-09-21T23:59:00")
    private LocalDateTime lateUntil;
    @Schema(description = "When true, clears lateUntil regardless of lateUntil field", example = "false")
    private Boolean clearLateUntil;
    @Schema(description = "Allowed file extensions/types", example = "[\"pdf\"]")
    private List<String> allowedFileTypes;
    @Schema(description = "Max size per file in bytes", example = "10485760")
    private Long maxFileSizeBytes;
    @Schema(description = "Max number of files per submission", example = "3")
    private Integer maxFileCount;
    @Schema(description = "Individual or Group; may be locked after submissions", example = "Individual",
            allowableValues = {"Individual", "Group"})
    private String submissionType;
    @Schema(description = "Group set id when submissionType=Group", example = "7")
    private Integer groupSetId;
    @Schema(description = "Must be true when shortening due date would affect students", example = "true")
    private Boolean confirmShortenDueDate;

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

    public Boolean getClearLateUntil() {
        return clearLateUntil;
    }

    public void setClearLateUntil(Boolean clearLateUntil) {
        this.clearLateUntil = clearLateUntil;
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

    public Boolean getConfirmShortenDueDate() {
        return confirmShortenDueDate;
    }

    public void setConfirmShortenDueDate(Boolean confirmShortenDueDate) {
        this.confirmShortenDueDate = confirmShortenDueDate;
    }
}
