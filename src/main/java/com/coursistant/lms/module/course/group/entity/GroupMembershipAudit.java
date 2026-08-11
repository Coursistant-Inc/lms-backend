package com.coursistant.lms.module.course.group.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GroupMembershipAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ACTOR_USER = "USER";
    public static final String ACTOR_ADMIN = "ADMIN";
    public static final String ACTOR_SYSTEM = "SYSTEM";

    public static final String JOIN_SELF = "JOIN_SELF";
    public static final String LEAVE_SELF = "LEAVE_SELF";
    public static final String SWITCH_SELF = "SWITCH_SELF";
    public static final String ASSIGN_STAFF = "ASSIGN_STAFF";
    public static final String MOVE_STAFF = "MOVE_STAFF";
    public static final String REMOVE_STAFF = "REMOVE_STAFF";
    public static final String DISTRIBUTE_RANDOM = "DISTRIBUTE_RANDOM";
    public static final String END_ON_DROP = "END_ON_DROP";
    public static final String END_ON_TA_PROMOTION = "END_ON_TA_PROMOTION";

    private Integer id;
    private Integer tenantId;
    private Integer courseId;
    private Integer groupSetId;
    private Integer groupId;
    private Integer targetUserId;
    private String actorType;
    private Integer actorUserId;
    private String action;
    private String beforeJson;
    private String afterJson;
    private String detailJson;
    private LocalDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getGroupSetId() {
        return groupSetId;
    }

    public void setGroupSetId(Integer groupSetId) {
        this.groupSetId = groupSetId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public Integer getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Integer actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public void setBeforeJson(String beforeJson) {
        this.beforeJson = beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public void setAfterJson(String afterJson) {
        this.afterJson = afterJson;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
