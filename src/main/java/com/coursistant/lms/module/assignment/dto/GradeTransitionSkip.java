package com.coursistant.lms.module.assignment.dto;

public class GradeTransitionSkip {

    private Integer studentUserId;
    private Integer groupId;
    private String reason;

    public GradeTransitionSkip() {
    }

    public GradeTransitionSkip(Integer studentUserId, String reason) {
        this.studentUserId = studentUserId;
        this.reason = reason;
    }

    public static GradeTransitionSkip forGroup(Integer groupId, String reason) {
        GradeTransitionSkip skip = new GradeTransitionSkip();
        skip.setGroupId(groupId);
        skip.setReason(reason);
        return skip;
    }

    public Integer getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Integer studentUserId) {
        this.studentUserId = studentUserId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
