package com.coursistant.lms.module.course.course.dto;

import java.time.LocalDate;

/**
 * Partial update. Only non-null whitelist fields are applied.
 * Use {@code clearDescription}/{@code clearLocation} to explicitly null those fields.
 * {@code tenantId} and instructor identity are rejected if present.
 */
public class PatchCourseRequest {

    private Integer tenantId;
    private Integer primaryInstructorUserId;
    private Integer instructorId;
    private String courseCode;
    private String title;
    private LocalDate termStartDate;
    private LocalDate termEndDate;
    private String description;
    private String location;
    private Boolean clearDescription;
    private Boolean clearLocation;

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
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

    public Boolean getClearDescription() {
        return clearDescription;
    }

    public void setClearDescription(Boolean clearDescription) {
        this.clearDescription = clearDescription;
    }

    public Boolean getClearLocation() {
        return clearLocation;
    }

    public void setClearLocation(Boolean clearLocation) {
        this.clearLocation = clearLocation;
    }
}
