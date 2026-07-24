package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One row of the grading roster. Individual assignments list students; Group assignments list
 * groups (with members / actual submitter).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GradingRosterItemResponse {

    private Integer studentUserId;
    private String studentName;
    private String studentEmail;
    private Integer groupId;
    private String groupName;
    private Integer memberCount;
    private List<GroupMemberSummary> members;
    private Integer actualSubmitterUserId;
    private String submissionStatus;
    private Integer submissionId;
    private Integer submissionVersionId;
    private Integer versionNo;
    private LocalDateTime submittedAt;
    private Boolean usedGraceBuffer;
    private Integer fileCount;
    /** {@code Ungraded} when no grade row exists, otherwise {@code Entered} / {@code Released}. */
    private String gradeStatus;
    private BigDecimal score;
    private LocalDateTime releasedAt;
    private Boolean hasAnnotatedFile;

    public Integer getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Integer studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
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

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
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

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
    }

    public Integer getSubmissionVersionId() {
        return submissionVersionId;
    }

    public void setSubmissionVersionId(Integer submissionVersionId) {
        this.submissionVersionId = submissionVersionId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getUsedGraceBuffer() {
        return usedGraceBuffer;
    }

    public void setUsedGraceBuffer(Boolean usedGraceBuffer) {
        this.usedGraceBuffer = usedGraceBuffer;
    }

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public String getGradeStatus() {
        return gradeStatus;
    }

    public void setGradeStatus(String gradeStatus) {
        this.gradeStatus = gradeStatus;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Boolean getHasAnnotatedFile() {
        return hasAnnotatedFile;
    }

    public void setHasAnnotatedFile(Boolean hasAnnotatedFile) {
        this.hasAnnotatedFile = hasAnnotatedFile;
    }
}
