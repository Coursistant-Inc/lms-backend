package com.coursistant.lms.module.assignment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of a bulk release / retract. Students whose grade was not in the required source
 * state are reported as skipped rather than failing the whole call.
 *
 * <p>Plan contract fields ({@code releasedCount}/{@code retractedCount},
 * {@code skippedStudentIds}, {@code alreadyReleasedStudentIds}) are exposed alongside the
 * richer {@code skipped} detail list for graders that need the prior status.</p>
 */
public class GradeTransitionResponse {

    private int changedCount;
    private List<Integer> changedStudentUserIds = new ArrayList<>();
    private List<Integer> changedGroupIds = new ArrayList<>();
    private List<GradeTransitionSkip> skipped = new ArrayList<>();

    public int getChangedCount() {
        return changedCount;
    }

    public void setChangedCount(int changedCount) {
        this.changedCount = changedCount;
    }

    @JsonProperty("releasedCount")
    public int getReleasedCount() {
        return changedCount;
    }

    @JsonProperty("retractedCount")
    public int getRetractedCount() {
        return changedCount;
    }

    public List<Integer> getChangedStudentUserIds() {
        return changedStudentUserIds;
    }

    public void setChangedStudentUserIds(List<Integer> changedStudentUserIds) {
        this.changedStudentUserIds = changedStudentUserIds;
    }

    public List<Integer> getChangedGroupIds() {
        return changedGroupIds;
    }

    public void setChangedGroupIds(List<Integer> changedGroupIds) {
        this.changedGroupIds = changedGroupIds;
    }

    public List<GradeTransitionSkip> getSkipped() {
        return skipped;
    }

    public void setSkipped(List<GradeTransitionSkip> skipped) {
        this.skipped = skipped;
    }

    @JsonProperty("skippedStudentIds")
    public List<Integer> getSkippedStudentIds() {
        return skipped.stream()
                .filter(s -> s != null && !"Released".equals(s.getReason()))
                .map(GradeTransitionSkip::getStudentUserId)
                .collect(Collectors.toList());
    }

    @JsonProperty("alreadyReleasedStudentIds")
    public List<Integer> getAlreadyReleasedStudentIds() {
        return skipped.stream()
                .filter(s -> s != null && "Released".equals(s.getReason()))
                .map(GradeTransitionSkip::getStudentUserId)
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public boolean isEmptyTransition() {
        return changedCount == 0;
    }
}
