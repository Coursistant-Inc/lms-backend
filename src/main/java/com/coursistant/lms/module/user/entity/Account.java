package com.coursistant.lms.module.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 角色标识
     * Role identifier
     */
    private String role;

    /** 新密码
     * New password
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
}
