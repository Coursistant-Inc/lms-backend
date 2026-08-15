package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One immutable submission attempt. {@code usedGraceBuffer} marks an attempt that landed in
 * the 5-minute grace window after the due date and is therefore not counted as late.
 */
@Schema(name = "SubmissionVersionResponse", description = "One immutable submission attempt")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionVersionResponse {

    private Integer id;
    private Integer submissionId;
    private Integer assignmentId;
    private Integer ownerUserId;
    private Integer versionNo;
    private Instant submittedAt;
    private Boolean usedGraceBuffer;
    private String submissionStatus;
    private Integer fileCount;
    private LocalDateTime receiptIssuedAt;
    private List<SubmissionFileResponse> files;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Integer ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getUsedGraceBuffer() {
        return usedGraceBuffer;
    }

    public void setUsedGraceBuffer(Boolean usedGraceBuffer) {
        this.usedGraceBuffer = usedGraceBuffer;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public LocalDateTime getReceiptIssuedAt() {
        return receiptIssuedAt;
    }

    public void setReceiptIssuedAt(LocalDateTime receiptIssuedAt) {
        this.receiptIssuedAt = receiptIssuedAt;
    }

    public List<SubmissionFileResponse> getFiles() {
        return files;
    }

    public void setFiles(List<SubmissionFileResponse> files) {
        this.files = files;
    }
}
