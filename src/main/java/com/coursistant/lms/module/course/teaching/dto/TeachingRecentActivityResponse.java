package com.coursistant.lms.module.course.teaching.dto;

import java.time.LocalDateTime;

/** Recent activity feed item. */
public class TeachingRecentActivityResponse {

    public static final String KIND_GROUP_MEMBERSHIP_CHANGE = "GroupMembershipChange";
    public static final String KIND_LATE_SUBMISSION = "LateSubmission";

    private String kind;
    private Integer courseId;
    private String courseCode;
    private String summary;
    private LocalDateTime occurredAt;
    private String timezone;
    private Integer assignmentId;
    private Integer groupSetId;
    private Integer groupId;
    private Integer targetUserId;

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Integer getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Integer assignmentId) { this.assignmentId = assignmentId; }
    public Integer getGroupSetId() { return groupSetId; }
    public void setGroupSetId(Integer groupSetId) { this.groupSetId = groupSetId; }
    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }
    public Integer getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Integer targetUserId) { this.targetUserId = targetUserId; }
}
