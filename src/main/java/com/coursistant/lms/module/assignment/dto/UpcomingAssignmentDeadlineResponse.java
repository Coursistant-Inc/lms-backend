package com.coursistant.lms.module.assignment.dto;

import java.time.LocalDateTime;

/** Dashboard card: assignment due within the upcoming window across enrolled courses. */
public class UpcomingAssignmentDeadlineResponse {

    private Integer courseId;
    private String courseCode;
    private Integer assignmentId;
    private String title;
    private LocalDateTime dueAtLocal;
    private String timezone;
    private String submissionStatus;

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public Integer getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Integer assignmentId) { this.assignmentId = assignmentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getDueAtLocal() { return dueAtLocal; }
    public void setDueAtLocal(LocalDateTime dueAtLocal) { this.dueAtLocal = dueAtLocal; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getSubmissionStatus() { return submissionStatus; }
    public void setSubmissionStatus(String submissionStatus) { this.submissionStatus = submissionStatus; }
}
