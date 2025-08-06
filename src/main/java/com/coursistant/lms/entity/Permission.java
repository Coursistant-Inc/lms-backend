package com.coursistant.lms.entity;

import java.io.Serializable;

public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer permissionId;
    private String permissionName;
    private String description;

    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (permissionId != null) sb.append("\"permissionId\":").append(permissionId).append(",");
        if (permissionName != null) sb.append("\"permissionName\":\"").append(permissionName).append("\",");
        if (description != null) sb.append("\"description\":\"").append(description).append("\",");
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); // Remove trailing comma
        sb.append("}");
        return sb.toString();
    }
}