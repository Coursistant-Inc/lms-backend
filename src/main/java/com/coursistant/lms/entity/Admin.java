package com.coursistant.lms.entity;

import com.coursistant.lms.utils.PasswordEncoderUtil;

import java.io.Serializable;

/**
 * 管理员
 * Administrator entity
 */
public class Admin extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID
     * Administrator ID
     */
    private Integer id;

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

    /** 电话
     * Phone number
     */
    private String phone;

    /** 邮箱
     * Email
     */
    private String email;

    /** 邀请码
     * Invitation code
     */
    private String invitation;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (username != null) sb.append("\"username\":\"").append(username).append("\",");
        if (password != null) sb.append("\"password\":\"").append(password).append("\",");
        if (name != null) sb.append("\"name\":\"").append(name).append("\",");
        if (avatar != null) sb.append("\"avatar\":\"").append(avatar).append("\",");
        if (role != null) sb.append("\"role\":\"").append(role).append("\",");
        if (phone != null) sb.append("\"phone\":\"").append(phone).append("\",");
        if (email != null) sb.append("\"email\":\"").append(email).append("\",");
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getInvitation() {
        return invitation;
    }

    @Override
    public void setInvitation(String invitation) {
        this.invitation = invitation;
    }
}
