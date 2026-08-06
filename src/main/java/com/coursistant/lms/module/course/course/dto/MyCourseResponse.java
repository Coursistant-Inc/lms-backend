package com.coursistant.lms.module.course.course.dto;

import java.time.LocalDateTime;

/** Enriched card for {@code GET /v2/me/courses}. */
public class MyCourseResponse {

    private Integer id;
    private Integer courseId;
    private String courseCode;
    private String title;
    private String name;
    private String description;
    private Integer tenantId;
    private String state;
    private String status;
    private String courseRole;
    private String role;
    private Boolean canGrade;
    private Boolean canPostAnnouncements;
    private Boolean canManageGroups;
    private Boolean canManageCourseEvents;
    private PrimaryInstructorSummary primaryInstructor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;

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

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCourseRole() {
        return courseRole;
    }

    public void setCourseRole(String courseRole) {
        this.courseRole = courseRole;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public PrimaryInstructorSummary getPrimaryInstructor() {
        return primaryInstructor;
    }

    public void setPrimaryInstructor(PrimaryInstructorSummary primaryInstructor) {
        this.primaryInstructor = primaryInstructor;
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

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
}
