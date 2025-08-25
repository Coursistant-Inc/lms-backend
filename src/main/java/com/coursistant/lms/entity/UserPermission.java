package com.coursistant.lms.entity;

import java.io.Serializable;

public class UserPermission implements Serializable { // Implement Serializable
    private static final long serialVersionUID = 1L; // Add serialVersionUID

    private Integer userPermissionId;
    private Integer userId;
    private Integer permissionId;
    private Boolean hasPermission;

    // Getters and Setters for UserPermission
    public Integer getUserPermissionId() {
        return userPermissionId;
    }

    public void setUserPermissionId(Integer userPermissionId) {
        this.userPermissionId = userPermissionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }

    public Boolean getHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(Boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (userPermissionId != null) sb.append("\"userPermissionId\":").append(userPermissionId).append(",");
        if (userId != null) sb.append("\"userId\":").append(userId).append(",");
        if (permissionId != null) sb.append("\"permissionId\":").append(permissionId).append(",");
        if (hasPermission != null) sb.append("\"hasPermission\":").append(hasPermission).append(",");
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); // Remove trailing comma
        sb.append("}");
        return sb.toString();
    }
}