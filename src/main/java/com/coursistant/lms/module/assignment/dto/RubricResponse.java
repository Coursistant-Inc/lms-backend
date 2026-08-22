package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Current rubric pointer for an assignment. {@code posted=false} means no rubric has ever
 * been uploaded (or the pointer was cleared); older versions stay in the version table.
 */
@Schema(name = "RubricResponse", description = "Current rubric pointer and metadata")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RubricResponse {

    private boolean posted;
    private Integer assignmentId;
    private Integer versionId;
    private Integer versionNo;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private Integer uploadedBy;
    private LocalDateTime uploadedAt;
    private Integer totalVersions;
    private Boolean canRestorePrevious;
    private boolean previewAvailable;
    private String downloadUrl;
    private String previewUrl;
    private Integer gradedAgainstPreviousRubricCount;

    public boolean isPosted() {
        return posted;
    }

    public void setPosted(boolean posted) {
        this.posted = posted;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Integer getTotalVersions() {
        return totalVersions;
    }

    public void setTotalVersions(Integer totalVersions) {
        this.totalVersions = totalVersions;
    }

    public Boolean getCanRestorePrevious() {
        return canRestorePrevious;
    }

    public void setCanRestorePrevious(Boolean canRestorePrevious) {
        this.canRestorePrevious = canRestorePrevious;
    }

    public boolean isPreviewAvailable() {
        return previewAvailable;
    }

    public void setPreviewAvailable(boolean previewAvailable) {
        this.previewAvailable = previewAvailable;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public Integer getGradedAgainstPreviousRubricCount() {
        return gradedAgainstPreviousRubricCount;
    }

    public void setGradedAgainstPreviousRubricCount(Integer gradedAgainstPreviousRubricCount) {
        this.gradedAgainstPreviousRubricCount = gradedAgainstPreviousRubricCount;
    }
}
