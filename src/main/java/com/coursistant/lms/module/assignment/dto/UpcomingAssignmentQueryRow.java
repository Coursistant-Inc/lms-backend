package com.coursistant.lms.module.assignment.dto;

import java.time.LocalDateTime;

/** Mapper row for cross-course upcoming Published assignments. */
public class UpcomingAssignmentQueryRow {

    private Integer id;
    private Integer courseId;
    private String courseCode;
    private String title;
    private LocalDateTime dueAt;
    private LocalDateTime lateUntil;
    private String submissionType;
    private Integer groupSetId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getLateUntil() { return lateUntil; }
    public void setLateUntil(LocalDateTime lateUntil) { this.lateUntil = lateUntil; }
    public String getSubmissionType() { return submissionType; }
    public void setSubmissionType(String submissionType) { this.submissionType = submissionType; }
    public Integer getGroupSetId() { return groupSetId; }
    public void setGroupSetId(Integer groupSetId) { this.groupSetId = groupSetId; }
}
