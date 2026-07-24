package com.coursistant.lms.module.course.group.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GroupSet implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    private String name;
    private Integer defaultCapacity;
    private LocalDateTime joinOpensAt;
    private LocalDateTime joinClosesAt;
    private Boolean locked;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDefaultCapacity() {
        return defaultCapacity;
    }

    public void setDefaultCapacity(Integer defaultCapacity) {
        this.defaultCapacity = defaultCapacity;
    }

    public LocalDateTime getJoinOpensAt() {
        return joinOpensAt;
    }

    public void setJoinOpensAt(LocalDateTime joinOpensAt) {
        this.joinOpensAt = joinOpensAt;
    }

    public LocalDateTime getJoinClosesAt() {
        return joinClosesAt;
    }

    public void setJoinClosesAt(LocalDateTime joinClosesAt) {
        this.joinClosesAt = joinClosesAt;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
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
