package com.coursistant.lms.module.course.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "CreateCourseRequest", description = "Create a new course")
public class CreateCourseRequest {

    private Integer tenantId;
    private String courseCode;
    private String title;
    private LocalDate termStartDate;
    private LocalDate termEndDate;
    private String description;
    private String location;
    /** Preferred field for admin create. */
    private Integer primaryInstructorUserId;
    /** Legacy alias of {@link #primaryInstructorUserId}. */
    private Integer instructorId;

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

    public Integer getPrimaryInstructorUserId() {
        return primaryInstructorUserId;
    }

    public void setPrimaryInstructorUserId(Integer primaryInstructorUserId) {
        this.primaryInstructorUserId = primaryInstructorUserId;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    /** Resolves primary instructor from preferred or legacy field. */
    public Integer resolvePrimaryInstructorUserId() {
        return primaryInstructorUserId != null ? primaryInstructorUserId : instructorId;
    }
}
