package com.coursistant.lms.module.course.group.dto;

import java.time.LocalDateTime;

public class PatchGroupSetRequest {
    private String name;
    private Integer defaultCapacity;
    private LocalDateTime joinOpensAt;
    private LocalDateTime joinClosesAt;
    private Boolean clearJoinOpensAt;
    private Boolean clearJoinClosesAt;
    private Boolean locked;
    private Boolean confirmCapacityShorten;
    private Boolean confirmWindowShorten;

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

    public Boolean getClearJoinOpensAt() {
        return clearJoinOpensAt;
    }

    public void setClearJoinOpensAt(Boolean clearJoinOpensAt) {
        this.clearJoinOpensAt = clearJoinOpensAt;
    }

    public Boolean getClearJoinClosesAt() {
        return clearJoinClosesAt;
    }

    public void setClearJoinClosesAt(Boolean clearJoinClosesAt) {
        this.clearJoinClosesAt = clearJoinClosesAt;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getConfirmCapacityShorten() {
        return confirmCapacityShorten;
    }

    public void setConfirmCapacityShorten(Boolean confirmCapacityShorten) {
        this.confirmCapacityShorten = confirmCapacityShorten;
    }

    public Boolean getConfirmWindowShorten() {
        return confirmWindowShorten;
    }

    public void setConfirmWindowShorten(Boolean confirmWindowShorten) {
        this.confirmWindowShorten = confirmWindowShorten;
    }
}
