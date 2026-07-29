package com.coursistant.lms.module.course.teaching.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Session template joined with course code and term for teaching activities. */
public class TeachingSessionRow {

    private Integer id;
    private Integer courseId;
    private String courseCode;
    private String type;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private LocalDate termStartDate;
    private LocalDate termEndDate;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getTermStartDate() { return termStartDate; }
    public void setTermStartDate(LocalDate termStartDate) { this.termStartDate = termStartDate; }
    public LocalDate getTermEndDate() { return termEndDate; }
    public void setTermEndDate(LocalDate termEndDate) { this.termEndDate = termEndDate; }
}
