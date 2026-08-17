package com.coursistant.lms.module.course.content.week.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CourseWeek implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    private String title;
    private Integer orderPosition;
    private String state;
    private Integer publicationVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getOrderPosition() {
        return orderPosition;
    }

    public void setOrderPosition(Integer orderPosition) {
        this.orderPosition = orderPosition;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getPublicationVersion() {
        return publicationVersion;
    }

    public void setPublicationVersion(Integer publicationVersion) {
        this.publicationVersion = publicationVersion;
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
