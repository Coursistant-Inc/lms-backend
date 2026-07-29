package com.coursistant.lms.module.course.teaching.dto;

import java.time.LocalDate;

/** Internal row: teaching course with term bounds for session expansion. */
public class TeachingCourseRow {

    private Integer id;
    private String courseCode;
    private String title;
    private String role;
    private LocalDate termStartDate;
    private LocalDate termEndDate;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDate getTermStartDate() { return termStartDate; }
    public void setTermStartDate(LocalDate termStartDate) { this.termStartDate = termStartDate; }
    public LocalDate getTermEndDate() { return termEndDate; }
    public void setTermEndDate(LocalDate termEndDate) { this.termEndDate = termEndDate; }
}
