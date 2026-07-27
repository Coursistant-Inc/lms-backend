package com.coursistant.lms.module.course.event.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dashboard activity card: expanded Course Session occurrence or Course Event.
 */
public class UpcomingCourseActivityResponse {

    public static final String SOURCE_SESSION = "Session";
    public static final String SOURCE_EVENT = "Event";
    public static final String TYPE_COURSE_EVENT = "CourseEvent";

    private Integer courseId;
    private String courseCode;
    private String type;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String source;
    private Integer sourceId;

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }
}
