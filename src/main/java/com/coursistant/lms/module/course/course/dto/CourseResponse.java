package com.coursistant.lms.module.course.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "CourseResponse", description = "Course detail view")
public class CourseResponse {

    private Integer id;
    private Integer courseId;
    private Integer tenantId;
    private String courseCode;
    private String title;
    private String name;
    private LocalDate termStartDate;
    private LocalDate termEndDate;
    private String description;
    private String location;
    private Integer instructorId;
    private PrimaryInstructorSummary primaryInstructor;
    private String state;
    private String status;
    private LocalDateTime archivedAt;
    private LocalDateTime gradingGraceEndsAt;
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

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public PrimaryInstructorSummary getPrimaryInstructor() {
        return primaryInstructor;
    }

    public void setPrimaryInstructor(PrimaryInstructorSummary primaryInstructor) {
        this.primaryInstructor = primaryInstructor;
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

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public LocalDateTime getGradingGraceEndsAt() {
        return gradingGraceEndsAt;
    }

    public void setGradingGraceEndsAt(LocalDateTime gradingGraceEndsAt) {
        this.gradingGraceEndsAt = gradingGraceEndsAt;
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
