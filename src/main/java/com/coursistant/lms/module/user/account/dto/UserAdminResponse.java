package com.coursistant.lms.module.user.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Safe admin/user read model — never includes password, newPassword, verification, or authVersion.
 */
@Schema(name = "UserAdminResponse", description = "Public user fields for admin read APIs")
public class UserAdminResponse {

    @Schema(description = "User ID", example = "385")
    private Integer id;

    @Schema(description = "Tenant this user belongs to", example = "1")
    private Integer tenantId;

    @Schema(description = "Username", example = "alex")
    private String username;

    @Schema(description = "Display / full name", example = "Alex Rivera")
    private String name;

    @Schema(description = "Email", example = "regtest1@example.com")
    private String email;

    @Schema(description = "Role identifier", example = "USER")
    private String role;

    @Schema(description = "Level", example = "STUDENT", allowableValues = {"STUDENT", "INSTRUCTOR"})
    private String level;

    @Schema(description = "Account status", example = "ACTIVE", allowableValues = {"ACTIVE", "DISABLED"})
    private String status;

    @Schema(description = "Avatar storage key (not an img URL)", example = "avatars/385/abc.jpg")
    private String avatar;

    @Schema(description = "Whether the user must change password on next login")
    private Boolean mustChangePassword;

    @Schema(description = "Whether the user receives email notifications")
    private Boolean emailNotifications;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
}
