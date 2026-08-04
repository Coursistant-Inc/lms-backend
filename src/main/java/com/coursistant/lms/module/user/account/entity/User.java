package com.coursistant.lms.module.user.account.entity;

import com.coursistant.lms.shared.util.PasswordEncoderUtil;

import java.io.Serializable;

/**
 * 管理员
 * User entity
 */
public class User extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID
     * User ID
     */
    private Integer id;

    /** Tenant this user belongs to. */
    private Integer tenantId;

    /** 用户名
     * Username
     */
    private String username;

    /** 密码
     * Password
     */
    private String password;

    /** 姓名
     * Full name
     */
    private String name;

    /** 头像
     * Avatar
     */
    private String avatar;

    /** 角色标识
     * Role identifier
     */
    private String role;

    /** 等级
     * Level
     */
    private String level;

    /** 邮箱
     * Email
     */
    private String email;

    private Boolean mustChangePassword;

    /** Whether the user receives email notifications (default true). */
    private Boolean emailNotifications;

    /** ACTIVE / DISABLED */
    private String status;

    private Integer authVersion;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (tenantId != null) sb.append("\"tenantId\":").append(tenantId).append(",");
        if (username != null) sb.append("\"username\":\"").append(username).append("\",");
        if (password != null) sb.append("\"password\":\"").append(password).append("\",");
        if (name != null) sb.append("\"name\":\"").append(name).append("\",");
        if (avatar != null) sb.append("\"avatar\":\"").append(avatar).append("\",");
        if (role != null) sb.append("\"role\":\"").append(role).append("\",");
        if (level != null) sb.append("\"level\":\"").append(level).append("\",");
        if (email != null) sb.append("\"email\":\"").append(email).append("\",");
        if (mustChangePassword != null) sb.append("\"mustChangePassword\":\"").append(mustChangePassword).append("\",");
        if (emailNotifications != null) sb.append("\"emailNotifications\":\"").append(emailNotifications).append("\",");
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); // 删除最后的逗号 / Remove trailing comma
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 设置加密密码
     * Set encrypted password
     */
    public void setEncryptPassword(String password) {
        this.password = PasswordEncoderUtil.encodePassword(password);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }

    @Override
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public void setRole(String role) {
        this.role = role;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email != null) {
            this.email = email.trim().toLowerCase(); // 去除空格并转换为小写 / Trim spaces and convert to lowercase
        }
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(Integer authVersion) {
        this.authVersion = authVersion;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

}
