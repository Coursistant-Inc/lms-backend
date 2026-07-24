package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceiptSummaryResponse {

    private Integer id;
    private LocalDateTime issuedAt;
    private List<ReceiptFileSummary> files;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public List<ReceiptFileSummary> getFiles() {
        return files;
    }

    public void setFiles(List<ReceiptFileSummary> files) {
        this.files = files;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReceiptFileSummary {
        private String originalName;
        private Long sizeBytes;
        private String checksumSha256;

        public String getOriginalName() {
            return originalName;
        }

        public void setOriginalName(String originalName) {
            this.originalName = originalName;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        public String getChecksumSha256() {
            return checksumSha256;
        }

        public void setChecksumSha256(String checksumSha256) {
            this.checksumSha256 = checksumSha256;
        }
    }
}
