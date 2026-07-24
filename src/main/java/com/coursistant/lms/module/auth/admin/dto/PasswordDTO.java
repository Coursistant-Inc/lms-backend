package com.coursistant.lms.module.auth.admin.dto;

/**
 * 角色用户父类
 * Base class for role-based users
 */
public class PasswordDTO {

    private String password;

    private String newPassword;

    private String email;

    private String role;

    private String code;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }



    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email != null) {
            this.email = email.trim().toLowerCase(); // 去除空格并转换为小写 / Trim spaces and convert to lowercase
        }
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
