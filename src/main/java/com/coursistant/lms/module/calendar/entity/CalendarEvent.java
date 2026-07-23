package com.coursistant.lms.module.calendar.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.coursistant.lms.module.assignment.entity.Assignment;

/**
 * 公告实体类
 * Assignment Entity
 */
public class CalendarEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;                    // 事件 ID
    private Integer userId;                // 用户 ID
    private String title;               // 标题
    private String description;         // 描述
    private Boolean isAllDay;           // 是否为全天事件
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")  // 表单、@RequestParam 参数绑定用
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC") // JSON 请求体用
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime endTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");

        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (userId != null) sb.append("\"userId\":").append(userId).append(",");
        if (title != null) sb.append("\"title\":\"").append(title).append("\",");
        if (description != null) sb.append("\"description\":\"").append(description).append("\",");
        if (startTime != null) sb.append("\"startTime\":\"").append(startTime).append("\",");
        if (endTime != null) sb.append("\"endTime\":\"").append(endTime).append("\",");
        if (isAllDay != null) sb.append("\"isAllDay\":").append(isAllDay).append(",");
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Boolean getAllDay() {
        return isAllDay;
    }

    public void setAllDay(Boolean allDay) {
        isAllDay = allDay;
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
