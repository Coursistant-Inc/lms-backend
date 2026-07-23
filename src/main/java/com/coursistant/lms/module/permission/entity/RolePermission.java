package com.coursistant.lms.module.permission.entity;

import java.io.Serializable;
import com.coursistant.lms.module.user.entity.User;

public class RolePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roleName; // Matches User.level
    private Integer permissionId;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Integer permissionId) {
        this.permissionId = permissionId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (roleName != null) sb.append("\"roleName\":\"").append(roleName).append("\",");
        if (permissionId != null) sb.append("\"permissionId\":").append(permissionId).append(",");
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); // Remove trailing comma
        sb.append("}");
        return sb.toString();
    }
}
