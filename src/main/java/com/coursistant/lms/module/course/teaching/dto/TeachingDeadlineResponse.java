package com.coursistant.lms.module.course.teaching.dto;

import java.time.LocalDateTime;

/** Upcoming deadline card (Assignment dueAt or Quiz closesAt). */
public class TeachingDeadlineResponse {

    public static final String KIND_ASSIGNMENT = "Assignment";
    public static final String KIND_QUIZ = "Quiz";

    private String kind;
    private Integer courseId;
    private String courseCode;
    private String title;
    private LocalDateTime atLocal;
    private String timezone;
    private Integer submittedCount;
    private Integer totalStudents;
    private Integer assignmentId;
    private Integer quizId;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getAtLocal() { return atLocal; }
    public void setAtLocal(LocalDateTime atLocal) { this.atLocal = atLocal; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Integer getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(Integer submittedCount) { this.submittedCount = submittedCount; }
    public Integer getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Integer totalStudents) { this.totalStudents = totalStudents; }
    public Integer getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Integer assignmentId) { this.assignmentId = assignmentId; }
    public Integer getQuizId() { return quizId; }
    public void setQuizId(Integer quizId) { this.quizId = quizId; }
}
