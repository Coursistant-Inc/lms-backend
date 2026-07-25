package com.coursistant.lms.module.course.content.week.dto;

import com.coursistant.lms.module.course.content.material.dto.MaterialResponse;

import java.time.LocalDateTime;
import java.util.List;

public class WeekResponse {
    private Integer id;
    private Integer courseId;
    private String title;
    private Integer orderPosition;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MaterialResponse> materials;

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

    public List<MaterialResponse> getMaterials() {
        return materials;
    }

    public void setMaterials(List<MaterialResponse> materials) {
        this.materials = materials;
    }
}
