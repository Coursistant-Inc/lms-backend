package com.coursistant.lms.module.course.teaching.dto;

import java.time.LocalDateTime;

/** Mapper aggregate row for grading-queue SQL. */
public class TeachingGradingQueueRow {

    private String kind;
    private Integer courseId;
    private String courseCode;
    private String title;
    private Integer pendingCount;
    private LocalDateTime oldestWaitingAt;
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
    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
    public LocalDateTime getOldestWaitingAt() { return oldestWaitingAt; }
    public void setOldestWaitingAt(LocalDateTime oldestWaitingAt) { this.oldestWaitingAt = oldestWaitingAt; }
    public Integer getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Integer assignmentId) { this.assignmentId = assignmentId; }
    public Integer getQuizId() { return quizId; }
    public void setQuizId(Integer quizId) { this.quizId = quizId; }
}
