package com.coursistant.lms.module.course.teaching.dto;

/** Slim teaching course card for Teacher Dashboard My Courses. */
public class TeachingCourseResponse {

    private Integer id;
    private String courseCode;
    private String title;
    private String role;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
