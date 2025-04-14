package com.coursistant.individual.entity;

import java.io.Serializable;

/**
 * Feedback Entity
 */
public class Feedback implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer userId;
    private Integer courseId;
    private String content;

    private String date;

    private String level;
    private String currentUrl;


    public Feedback() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (userId != null) sb.append("\"userId\":").append(userId).append(",");
        if (courseId != null) sb.append("\"courseId\":").append(courseId).append(",");
        if (content != null) sb.append("\"content\":\"").append(content).append("\",");
        if (level != null) sb.append("\"level\":\"").append(level).append("\",");
        if (currentUrl != null) sb.append("\"currentURL\":\"").append(currentUrl).append("\",");

        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
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

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

     public String getDate() {
        return date;
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }


    public void setDate(String date) {
        this.date = date;
    }
}
