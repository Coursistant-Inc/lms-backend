package com.coursistant.lms.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 公告实体类
 * Assignment Entity
 */
public class CourseSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;                   // 排课 ID
    private Integer courseId;             // 课程 ID
    private Integer professorId;          // 教授 ID
    private Integer weekday;           // 星期几 (1~7)
    private LocalTime startTime;       // 上课开始时间
    private LocalTime endTime;         // 上课结束时间
    private String location;           // 上课地点
    private String timezone;
    private LocalDate startDate;       // 开始日期
    private LocalDate endDate;         // 结束日期
    private LocalDate createdAt;       // 创建时间
    private LocalDate updatedAt;       // 更新时间


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");

        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (courseId != null) sb.append("\"courseId\":").append(courseId).append(",");
        if (professorId != null) sb.append("\"professorId\":").append(professorId).append(",");
        if (weekday != null) sb.append("\"weekday\":").append(weekday).append(",");
        if (startTime != null) sb.append("\"startTime\":\"").append(startTime).append("\",");
        if (endTime != null) sb.append("\"endTime\":\"").append(endTime).append("\",");
        if (timezone != null) sb.append("\"timezone\":\"").append(timezone).append("\",");
        if (location != null) sb.append("\"location\":\"").append(location).append("\",");
        if (startDate != null) sb.append("\"startDate\":\"").append(startDate).append("\",");
        if (endDate != null) sb.append("\"endDate\":\"").append(endDate).append("\",");
        if (createdAt != null) sb.append("\"createdAt\":\"").append(createdAt).append("\",");
        if (updatedAt != null) sb.append("\"updatedAt\":\"").append(updatedAt).append("\",");

        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1); // 去掉最后一个逗号
        }

        sb.append("}");
        return sb.toString();
    }


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

    public Integer getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Integer professorId) {
        this.professorId = professorId;
    }

    public Integer getWeekday() {
        return weekday;
    }

    public void setWeekday(Integer weekday) {
        this.weekday = weekday;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
