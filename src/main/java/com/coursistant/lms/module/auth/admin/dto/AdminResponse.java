package com.coursistant.lms.module.auth.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Safe admin read model — never includes password, authVersion, or invitation.
 */
@Schema(name = "AdminResponse", description = "Public admin profile fields (no secrets)")
public class AdminResponse {

    @Schema(description = "Admin id", example = "1")
    private Integer id;
    @Schema(description = "Username", example = "sysadmin")
    private String username;
    @Schema(description = "Display name", example = "System Admin")
    private String name;
    @Schema(description = "Phone", example = "+1-555-0100")
    private String phone;
    @Schema(description = "Email", example = "admin@example.com")
    private String email;
    @Schema(description = "Avatar URL or object key")
    private String avatar;
    @Schema(description = "Role", example = "SYSTEM_ADMIN")
    private String role;
    @Schema(description = "Account status", example = "ACTIVE")
    private String status;

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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
