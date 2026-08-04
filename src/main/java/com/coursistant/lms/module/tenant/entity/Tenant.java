package com.coursistant.lms.module.tenant.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private String timezone;
    private String status;
    private Integer securityVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSecurityVersion() {
        return securityVersion;
    }

    public void setSecurityVersion(Integer securityVersion) {
        this.securityVersion = securityVersion;
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
