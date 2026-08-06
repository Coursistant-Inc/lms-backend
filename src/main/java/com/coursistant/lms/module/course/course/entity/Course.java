package com.coursistant.lms.module.course.course.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer tenantId;
    private String courseCode;
    private String title;
    private LocalDate termStartDate;
    private LocalDate termEndDate;
    private String description;
    private String location;
    private Integer instructorId;
    private String state;
    private LocalDateTime archivedAt;
    private String archivedByActorType;
    private Integer archivedByActorId;
    private Integer creatorId;
    private String creatorActorType;
    private Integer creatorActorId;
    private String creatorRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public LocalDate getTermStartDate() {
        return termStartDate;
    }

    public void setTermStartDate(LocalDate termStartDate) {
        this.termStartDate = termStartDate;
    }

    public LocalDate getTermEndDate() {
        return termEndDate;
    }

    public void setTermEndDate(LocalDate termEndDate) {
        this.termEndDate = termEndDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchivedByActorType() {
        return archivedByActorType;
    }

    public void setArchivedByActorType(String archivedByActorType) {
        this.archivedByActorType = archivedByActorType;
    }

    public Integer getArchivedByActorId() {
        return archivedByActorId;
    }

    public void setArchivedByActorId(Integer archivedByActorId) {
        this.archivedByActorId = archivedByActorId;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorActorType() {
        return creatorActorType;
    }

    public void setCreatorActorType(String creatorActorType) {
        this.creatorActorType = creatorActorType;
    }

    public Integer getCreatorActorId() {
        return creatorActorId;
    }

    public void setCreatorActorId(Integer creatorActorId) {
        this.creatorActorId = creatorActorId;
    }

    public String getCreatorRole() {
        return creatorRole;
    }

    public void setCreatorRole(String creatorRole) {
        this.creatorRole = creatorRole;
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
