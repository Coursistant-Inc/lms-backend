package com.coursistant.lms.module.course.group.dto;

import java.time.LocalDateTime;

public class CreateGroupSetRequest {
    private String name;
    private Integer defaultCapacity;
    private LocalDateTime joinOpensAt;
    private LocalDateTime joinClosesAt;
    private Boolean locked;

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
}
