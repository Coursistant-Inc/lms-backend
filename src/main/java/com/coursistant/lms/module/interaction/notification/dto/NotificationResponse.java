package com.coursistant.lms.module.interaction.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "NotificationResponse", description = "One in-app notification for the current user")
public class NotificationResponse {

    @Schema(description = "Notification id", example = "42")
    private Integer notificationId;
    @Schema(description = "Tenant id", example = "1")
    private Integer tenantId;
    @Schema(description = "Recipient user id (always the caller)", example = "10")
    private Integer recipientUserId;
    @Schema(description = "Course id when the event is course-scoped", example = "7")
    private Integer courseId;
    @Schema(description = "Course code when available", example = "CS101")
    private String courseCode;
    @Schema(description = "Stable notification type", example = "ASSIGNMENT_PUBLISHED",
            allowableValues = {
                    "ANNOUNCEMENT_POSTED",
                    "ASSIGNMENT_PUBLISHED",
                    "ASSIGNMENT_SUBMISSION_RECEIVED",
                    "ASSIGNMENT_GRADE_RELEASED",
                    "QUIZ_GRADE_RELEASED",
                    "ASSIGNMENT_GRADE_CORRECTED",
                    "QUIZ_GRADE_CORRECTED",
                    "WEEK_PUBLISHED",
                    "ASSIGNMENT_SCHEDULE_CHANGED",
                    "QUIZ_PUBLISHED",
                    "QUIZ_SCHEDULE_CHANGED",
                    "QUIZ_TIME_LIMIT_CHANGED",
                    "COURSE_EVENT_CREATED",
                    "GROUP_MEMBER_ADDED",
                    "GROUP_MEMBER_REMOVED",
                    "GROUP_MEMBER_MOVED"
            })
    private String notificationType;
    @Schema(description = "Student-facing message. Never includes numeric scores.",
            example = "New assignment published: Homework 1")
    private String message;
    @Schema(description = "Subject kind used for availability and deep links", example = "ASSIGNMENT",
            allowableValues = {
                    "ANNOUNCEMENT",
                    "ASSIGNMENT",
                    "QUIZ",
                    "ASSIGNMENT_GRADE",
                    "QUIZ_GRADE",
                    "ASSIGNMENT_SUBMISSION",
                    "WEEK",
                    "COURSE_EVENT",
                    "GROUP_SET"
            })
    private String subjectType;
    @Schema(description = "Subject id (assignment/quiz/week/announcement/etc.)", example = "12")
    private Integer subjectId;
    @Schema(description = "Frontend path (not an absolute URL)",
            example = "/courses/7/assignments/12")
    private String deepLink;
    @Schema(description = "Created at (UTC)", format = "date-time", example = "2026-08-17T18:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "Read at (UTC). Null when unread.", format = "date-time")
    private LocalDateTime readAt;
    @Schema(description = "Whether the subject is currently reachable for this viewer. "
                    + "AVAILABLE means the deep link is safe to open; NO_LONGER_AVAILABLE "
                    + "means the subject was unpublished, deleted, or the viewer lost access.",
            example = "AVAILABLE",
            allowableValues = {"AVAILABLE", "NO_LONGER_AVAILABLE"})
    private String availability;

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Integer recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }
}
