package com.coursistant.lms.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小组加入请求实体类
 * Entity for group join request
 */
public class GroupJoinRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Integer id;

    /** 小组 ID */
    private Integer groupId;

    /** 申请者用户 ID */
    private Integer userId;

    /** 课程 ID */
    private Integer courseId;

    /** 作业 ID */
    private Integer assignmentId;

    /** 请求状态：'pending', 'approved', 'rejected' */
    private String status;

    /** 申请时间 */
    private LocalDateTime requestTime;

    /** 审批者 ID */
    private Integer approverId;

    /** 审批时间 */
    private LocalDateTime approveTime;

    public GroupJoinRequest() {}

    public GroupJoinRequest(Integer groupId, Integer userId, Integer courseId, Integer assignmentId) {
        this.groupId = groupId;
        this.userId = userId;
        this.courseId = courseId;
        this.assignmentId = assignmentId;
        this.status = "pending";
        this.requestTime = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public Integer getApproverId() {
        return approverId;
    }

    public void setApproverId(Integer approverId) {
        this.approverId = approverId;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    @Override
    public String toString() {
        return "GroupJoinRequest{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", courseId=" + courseId +
                ", assignmentId=" + assignmentId +
                ", status='" + status + '\'' +
                ", requestTime=" + requestTime +
                ", approverId=" + approverId +
                ", approveTime=" + approveTime +
                '}';
    }
}
