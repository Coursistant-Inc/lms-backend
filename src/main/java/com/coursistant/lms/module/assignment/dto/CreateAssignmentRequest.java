package com.coursistant.lms.module.assignment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Create payload for an Individual assignment. {@code dueAt}/{@code lateUntil} are wall-clock
 * times in the caller's {@code X-Timezone} and are converted to UTC before persisting.
 */
public class CreateAssignmentRequest {

    private String title;
    private String description;
    private BigDecimal pointsPossible;
    private LocalDateTime dueAt;
    private LocalDateTime lateUntil;
    private List<String> allowedFileTypes;
    private Long maxFileSizeBytes;
    private Integer maxFileCount;

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
}
