package com.coursistant.lms.entity;

/**
 * 角色用户父类
 * Base class for role-based users
 */
public class Account {
    private Integer id;

    /** 用户名
     * Username
     */
    private String username;

    /** 名称
     * Full name
     */
    private String name;

    /** 密码
     * Password
     */
    private String password;

    /** 角色标识
     * Role identifier
     */
    private String role;

    /** 新密码
     * New password
     */
    private String newPassword;

    /** 头像
     * Avatar
     */
    private String avatar;

    /** 邀请码
     * Invitation code
     */
    private String invitation;

    /** 认证 Token
     * Authentication token
     */
    private String accessToken;

    private String refreshToken;

    /** 等级
     * Level
     */
    private String level;

    /** 邮箱
     * Email
     */
    private String email;

    /** 邮箱验证码
     * Email verification code
     */
    private String verification;

    private String type;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getInvitation() {
        return invitation;
    }

    public void setInvitation(String invitation) {
        this.invitation = invitation;
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

    public String getVerification() {
        return verification;
    }

    public void setVerification(String verification) {
        this.verification = verification;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    
}
