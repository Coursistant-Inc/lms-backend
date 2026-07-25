package com.coursistant.lms.module.course.content.syllabus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Response for {@code GET /v2/courses/{courseId}/syllabus}.
 * When {@code posted} is false, no syllabus has ever been uploaded and all
 * other fields are omitted. {@code canRestorePrevious} is only populated for
 * the instructor view (null/omitted for students and TAs).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyllabusResponse {

    private boolean posted;
    private Integer versionId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private Integer uploadedBy;
    private LocalDateTime uploadedAt;
    private Boolean canRestorePrevious;

    public boolean isPosted() {
        return posted;
    }

    public void setPosted(boolean posted) {
        this.posted = posted;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
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

    public Boolean getCanRestorePrevious() {
        return canRestorePrevious;
    }

    public void setCanRestorePrevious(Boolean canRestorePrevious) {
        this.canRestorePrevious = canRestorePrevious;
    }
}
