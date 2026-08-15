package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A student's submission state for one assignment: the current version (if any), the derived
 * status, and whether another attempt is still accepted.
 */
@Schema(name = "SubmissionResponse", description = "Submission state for one assignment")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionResponse {

    private Integer submissionId;
    private Integer assignmentId;
    private Integer ownerUserId;
    private Integer groupId;
    private String groupName;
    private List<GroupMemberSummary> members;
    private Integer actualSubmitterUserId;
    /** Null when eligible; {@code NO_GROUP_MEMBERSHIP} when the viewer has no group. */
    private String submissionEligibility;
    private String submissionStatus;
    private Instant dueAtUtc;
    private Instant lateUntilUtc;
    private LocalDateTime dueAtLocal;
    private LocalDateTime lateUntilLocal;
    private String timezone;
    private Boolean windowOpen;
    private Boolean acceptingSubmissions;
    private Boolean graceWindowActive;
    private Boolean submitFrozen;
    private Integer maxFileCount;
    private Long maxFileSizeBytes;
    private List<String> allowedFileTypes;
    private Integer totalVersions;
    private SubmissionVersionResponse currentVersion;
    private ReceiptSummaryResponse receipt;
    private String deadlineOutcome;
    private Boolean usedGraceBuffer;
    private List<StagingFileResponse> stagingFiles;

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

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<GroupMemberSummary> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberSummary> members) {
        this.members = members;
    }

    public Integer getActualSubmitterUserId() {
        return actualSubmitterUserId;
    }

    public void setActualSubmitterUserId(Integer actualSubmitterUserId) {
        this.actualSubmitterUserId = actualSubmitterUserId;
    }

    public String getSubmissionEligibility() {
        return submissionEligibility;
    }

    public void setSubmissionEligibility(String submissionEligibility) {
        this.submissionEligibility = submissionEligibility;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public Instant getDueAtUtc() {
        return dueAtUtc;
    }

    public void setDueAtUtc(Instant dueAtUtc) {
        this.dueAtUtc = dueAtUtc;
    }

    public Instant getLateUntilUtc() {
        return lateUntilUtc;
    }

    public void setLateUntilUtc(Instant lateUntilUtc) {
        this.lateUntilUtc = lateUntilUtc;
    }

    public LocalDateTime getDueAtLocal() {
        return dueAtLocal;
    }

    public void setDueAtLocal(LocalDateTime dueAtLocal) {
        this.dueAtLocal = dueAtLocal;
    }

    public LocalDateTime getLateUntilLocal() {
        return lateUntilLocal;
    }

    public void setLateUntilLocal(LocalDateTime lateUntilLocal) {
        this.lateUntilLocal = lateUntilLocal;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getWindowOpen() {
        return windowOpen;
    }

    public void setWindowOpen(Boolean windowOpen) {
        this.windowOpen = windowOpen;
    }

    public Boolean getAcceptingSubmissions() {
        return acceptingSubmissions;
    }

    public void setAcceptingSubmissions(Boolean acceptingSubmissions) {
        this.acceptingSubmissions = acceptingSubmissions;
    }

    public Boolean getGraceWindowActive() {
        return graceWindowActive;
    }

    public void setGraceWindowActive(Boolean graceWindowActive) {
        this.graceWindowActive = graceWindowActive;
    }

    public Boolean getSubmitFrozen() {
        return submitFrozen;
    }

    public void setSubmitFrozen(Boolean submitFrozen) {
        this.submitFrozen = submitFrozen;
    }

    public Integer getMaxFileCount() {
        return maxFileCount;
    }

    public void setMaxFileCount(Integer maxFileCount) {
        this.maxFileCount = maxFileCount;
    }

    public Long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedFileTypes() {
        return allowedFileTypes;
    }

    public void setAllowedFileTypes(List<String> allowedFileTypes) {
        this.allowedFileTypes = allowedFileTypes;
    }

    public Integer getTotalVersions() {
        return totalVersions;
    }

    public void setTotalVersions(Integer totalVersions) {
        this.totalVersions = totalVersions;
    }

    public SubmissionVersionResponse getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(SubmissionVersionResponse currentVersion) {
        this.currentVersion = currentVersion;
    }

    public ReceiptSummaryResponse getReceipt() {
        return receipt;
    }

    public void setReceipt(ReceiptSummaryResponse receipt) {
        this.receipt = receipt;
    }

    public String getDeadlineOutcome() {
        return deadlineOutcome;
    }

    public void setDeadlineOutcome(String deadlineOutcome) {
        this.deadlineOutcome = deadlineOutcome;
    }

    public Boolean getUsedGraceBuffer() {
        return usedGraceBuffer;
    }

    public void setUsedGraceBuffer(Boolean usedGraceBuffer) {
        this.usedGraceBuffer = usedGraceBuffer;
    }

    public List<StagingFileResponse> getStagingFiles() {
        return stagingFiles;
    }

    public void setStagingFiles(List<StagingFileResponse> stagingFiles) {
        this.stagingFiles = stagingFiles;
    }
}
