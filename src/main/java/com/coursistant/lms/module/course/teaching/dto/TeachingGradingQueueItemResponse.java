package com.coursistant.lms.module.course.teaching.dto;

import java.time.LocalDateTime;

/** Grading Queue card for Teacher Dashboard. */
public class TeachingGradingQueueItemResponse {

    public static final String KIND_ASSIGNMENT_UNGRADED = "AssignmentUngraded";
    public static final String KIND_QUIZ_MANUAL_PENDING = "QuizManualPending";
    public static final String KIND_ASSIGNMENT_AWAITING_RELEASE = "AssignmentAwaitingRelease";
    public static final String KIND_QUIZ_AWAITING_RELEASE = "QuizAwaitingRelease";

    private String kind;
    private Integer courseId;
    private String courseCode;
    private String title;
    private Integer pendingCount;
    private LocalDateTime oldestWaitingAt;
    private Long waitingMinutes;
    private String timezone;
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
    public Long getWaitingMinutes() { return waitingMinutes; }
    public void setWaitingMinutes(Long waitingMinutes) { this.waitingMinutes = waitingMinutes; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Integer getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Integer assignmentId) { this.assignmentId = assignmentId; }
    public Integer getQuizId() { return quizId; }
    public void setQuizId(Integer quizId) { this.quizId = quizId; }
}
