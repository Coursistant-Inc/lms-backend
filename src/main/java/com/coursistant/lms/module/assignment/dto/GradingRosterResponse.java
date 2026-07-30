package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GradingRosterResponse {

    private Integer assignmentId;
    private String assignmentTitle;
    private BigDecimal pointsPossible;
    private Instant dueAtUtc;
    private Instant lateUntilUtc;
    private LocalDateTime dueAtLocal;
    private LocalDateTime lateUntilLocal;
    private String timezone;
    private Integer totalStudents;
    private Integer submittedCount;
    private Integer lateCount;
    private Integer notSubmittedCount;
    private Integer ungradedCount;
    private Integer enteredCount;
    private Integer releasedCount;
    private Boolean gradingWritable;
    private LocalDateTime gradingWritableUntil;
    private List<GradingRosterItemResponse> items = new ArrayList<>();

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public BigDecimal getPointsPossible() {
        return pointsPossible;
    }

    public void setPointsPossible(BigDecimal pointsPossible) {
        this.pointsPossible = pointsPossible;
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

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Integer getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(Integer submittedCount) {
        this.submittedCount = submittedCount;
    }

    public Integer getLateCount() {
        return lateCount;
    }

    public void setLateCount(Integer lateCount) {
        this.lateCount = lateCount;
    }

    public Integer getNotSubmittedCount() {
        return notSubmittedCount;
    }

    public void setNotSubmittedCount(Integer notSubmittedCount) {
        this.notSubmittedCount = notSubmittedCount;
    }

    public Integer getUngradedCount() {
        return ungradedCount;
    }

    public void setUngradedCount(Integer ungradedCount) {
        this.ungradedCount = ungradedCount;
    }

    public Integer getEnteredCount() {
        return enteredCount;
    }

    public void setEnteredCount(Integer enteredCount) {
        this.enteredCount = enteredCount;
    }

    public Integer getReleasedCount() {
        return releasedCount;
    }

    public void setReleasedCount(Integer releasedCount) {
        this.releasedCount = releasedCount;
    }

    public Boolean getGradingWritable() {
        return gradingWritable;
    }

    public void setGradingWritable(Boolean gradingWritable) {
        this.gradingWritable = gradingWritable;
    }

    public LocalDateTime getGradingWritableUntil() {
        return gradingWritableUntil;
    }

    public void setGradingWritableUntil(LocalDateTime gradingWritableUntil) {
        this.gradingWritableUntil = gradingWritableUntil;
    }

    public List<GradingRosterItemResponse> getItems() {
        return items;
    }

    public void setItems(List<GradingRosterItemResponse> items) {
        this.items = items;
    }
}
