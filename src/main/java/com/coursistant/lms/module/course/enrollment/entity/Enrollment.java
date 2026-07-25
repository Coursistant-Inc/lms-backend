package com.coursistant.lms.module.course.enrollment.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Enrollment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    private Integer userId;
    private String courseRole;
    private Boolean canGrade;
    private Boolean canPostAnnouncements;
    private Boolean canManageGroups;
    private Boolean canManageCourseEvents;
    private Boolean active;
    private LocalDateTime enrolledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getCourseRole() {
        return courseRole;
    }

    public void setCourseRole(String courseRole) {
        this.courseRole = courseRole;
    }

    public Boolean getCanGrade() {
        return canGrade;
    }

    public void setCanGrade(Boolean canGrade) {
        this.canGrade = canGrade;
    }

    public Boolean getCanPostAnnouncements() {
        return canPostAnnouncements;
    }

    public void setCanPostAnnouncements(Boolean canPostAnnouncements) {
        this.canPostAnnouncements = canPostAnnouncements;
    }

    public Boolean getCanManageGroups() {
        return canManageGroups;
    }

    public void setCanManageGroups(Boolean canManageGroups) {
        this.canManageGroups = canManageGroups;
    }

    public Boolean getCanManageCourseEvents() {
        return canManageCourseEvents;
    }

    public void setCanManageCourseEvents(Boolean canManageCourseEvents) {
        this.canManageCourseEvents = canManageCourseEvents;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
