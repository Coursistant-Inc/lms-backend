package com.coursistant.lms.module.assignment.dto;

public class GradeTransitionSkip {

    private Integer studentUserId;
    private String reason;

    public GradeTransitionSkip() {
    }

    public GradeTransitionSkip(Integer studentUserId, String reason) {
        this.studentUserId = studentUserId;
        this.reason = reason;
    }

    public Integer getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Integer studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
