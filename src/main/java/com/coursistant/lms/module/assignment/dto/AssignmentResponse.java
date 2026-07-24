package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Assignment payload shared by list and detail responses. Role-specific blocks are filled by
 * the assemblers: staff callers receive the roster/grading counters, students receive their own
 * submission state. Unset fields are omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentResponse {

    private Integer id;
    private Integer courseId;
    private String title;
    private String description;
    private BigDecimal pointsPossible;

    /** UTC. */
    private LocalDateTime dueAt;
    /** UTC, null when the assignment does not accept late submissions. */
    private LocalDateTime lateUntil;
    /** {@code dueAt} rendered in the caller's X-Timezone. */
    private LocalDateTime dueAtLocal;
    private LocalDateTime lateUntilLocal;
    private String timezone;
    /** Alias of {@link #timezone} for the Part 5 contract. */
    private String timezoneLabel;

    private String submissionType;
    private Integer groupSetId;
    private List<String> allowedFileTypes;
    private Long maxFileSizeBytes;
    private Integer maxFileCount;
    private String state;

    private Boolean hasRubric;
    private Integer currentRubricVersionId;
    private Integer currentRubricVersionNo;

    private List<AssignmentAttachmentResponse> attachments;

    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Staff view ---
    private Integer activeStudentCount;
    private Integer submissionCount;
    private Integer gradedCount;
    private Integer releasedCount;
    private Boolean canEditStructure;

    // --- Student view ---
    private String submissionStatus;
    private Integer groupId;
    private String groupName;
    /** Null when eligible; {@code NO_GROUP_MEMBERSHIP} when the viewer has no group on a Group assignment. */
    private String submissionEligibility;
    private LocalDateTime submittedAt;
    private Integer versionNo;
    private Boolean usedGraceBuffer;
    private Boolean windowOpen;
    private Boolean acceptingSubmissions;
    private Integer stagedFileCount;
    private Boolean gradeReleased;
    private BigDecimal score;
    private String gradeDisplay;
    private StudentGradeViewResponse grade;
    private String feedback;
    private ReceiptSummaryResponse receipt;
    private RubricResponse currentRubric;
    private LatePolicyResponse latePolicy;
    private SubmissionAreaResponse submissionArea;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

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
        this.timezoneLabel = timezone;
    }

    public String getTimezoneLabel() {
        return timezoneLabel;
    }

    public void setTimezoneLabel(String timezoneLabel) {
        this.timezoneLabel = timezoneLabel;
        this.timezone = timezoneLabel;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getHasRubric() {
        return hasRubric;
    }

    public void setHasRubric(Boolean hasRubric) {
        this.hasRubric = hasRubric;
    }

    public Integer getCurrentRubricVersionId() {
        return currentRubricVersionId;
    }

    public void setCurrentRubricVersionId(Integer currentRubricVersionId) {
        this.currentRubricVersionId = currentRubricVersionId;
    }

    public Integer getCurrentRubricVersionNo() {
        return currentRubricVersionNo;
    }

    public void setCurrentRubricVersionNo(Integer currentRubricVersionNo) {
        this.currentRubricVersionNo = currentRubricVersionNo;
    }

    public List<AssignmentAttachmentResponse> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AssignmentAttachmentResponse> attachments) {
        this.attachments = attachments;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getActiveStudentCount() {
        return activeStudentCount;
    }

    public void setActiveStudentCount(Integer activeStudentCount) {
        this.activeStudentCount = activeStudentCount;
    }

    public Integer getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(Integer submissionCount) {
        this.submissionCount = submissionCount;
    }

    public Integer getGradedCount() {
        return gradedCount;
    }

    public void setGradedCount(Integer gradedCount) {
        this.gradedCount = gradedCount;
    }

    public Integer getReleasedCount() {
        return releasedCount;
    }

    public void setReleasedCount(Integer releasedCount) {
        this.releasedCount = releasedCount;
    }

    public Boolean getCanEditStructure() {
        return canEditStructure;
    }

    public void setCanEditStructure(Boolean canEditStructure) {
        this.canEditStructure = canEditStructure;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
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

    public String getSubmissionEligibility() {
        return submissionEligibility;
    }

    public void setSubmissionEligibility(String submissionEligibility) {
        this.submissionEligibility = submissionEligibility;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Boolean getUsedGraceBuffer() {
        return usedGraceBuffer;
    }

    public void setUsedGraceBuffer(Boolean usedGraceBuffer) {
        this.usedGraceBuffer = usedGraceBuffer;
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

    public Integer getStagedFileCount() {
        return stagedFileCount;
    }

    public void setStagedFileCount(Integer stagedFileCount) {
        this.stagedFileCount = stagedFileCount;
    }

    public Boolean getGradeReleased() {
        return gradeReleased;
    }

    public void setGradeReleased(Boolean gradeReleased) {
        this.gradeReleased = gradeReleased;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getGradeDisplay() {
        return gradeDisplay;
    }

    public void setGradeDisplay(String gradeDisplay) {
        this.gradeDisplay = gradeDisplay;
    }

    public StudentGradeViewResponse getGrade() {
        return grade;
    }

    public void setGrade(StudentGradeViewResponse grade) {
        this.grade = grade;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public ReceiptSummaryResponse getReceipt() {
        return receipt;
    }

    public void setReceipt(ReceiptSummaryResponse receipt) {
        this.receipt = receipt;
    }

    public RubricResponse getCurrentRubric() {
        return currentRubric;
    }

    public void setCurrentRubric(RubricResponse currentRubric) {
        this.currentRubric = currentRubric;
    }

    public LatePolicyResponse getLatePolicy() {
        return latePolicy;
    }

    public void setLatePolicy(LatePolicyResponse latePolicy) {
        this.latePolicy = latePolicy;
    }

    public SubmissionAreaResponse getSubmissionArea() {
        return submissionArea;
    }

    public void setSubmissionArea(SubmissionAreaResponse submissionArea) {
        this.submissionArea = submissionArea;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LatePolicyResponse {
        private Boolean acceptsLate;
        private LocalDateTime lateUntil;

        public Boolean getAcceptsLate() {
            return acceptsLate;
        }

        public void setAcceptsLate(Boolean acceptsLate) {
            this.acceptsLate = acceptsLate;
        }

        public LocalDateTime getLateUntil() {
            return lateUntil;
        }

        public void setLateUntil(LocalDateTime lateUntil) {
            this.lateUntil = lateUntil;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubmissionAreaResponse {
        private Integer maxFileCount;
        private Long maxFileSizeBytes;
        private List<String> allowedFileTypes;

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
    }
}
