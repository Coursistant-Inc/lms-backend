package com.coursistant.lms.module.course.group.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CourseGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer groupSetId;
    private Integer courseId;
    private String name;
    private Integer capacityOverride;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupSetId() {
        return groupSetId;
    }

    public void setGroupSetId(Integer groupSetId) {
        this.groupSetId = groupSetId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacityOverride() {
        return capacityOverride;
    }

    public void setCapacityOverride(Integer capacityOverride) {
        this.capacityOverride = capacityOverride;
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
