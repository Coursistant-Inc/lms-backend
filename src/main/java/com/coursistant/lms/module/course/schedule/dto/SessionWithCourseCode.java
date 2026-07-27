package com.coursistant.lms.module.course.schedule.dto;

import java.time.LocalTime;

/** Mapper row: course_session joined with course code for dashboard expansion. */
public class SessionWithCourseCode {

    private Integer id;
    private Integer courseId;
    private String courseCode;
    private String type;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;

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
}
