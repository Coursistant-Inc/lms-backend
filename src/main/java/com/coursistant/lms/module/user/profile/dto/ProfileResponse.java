package com.coursistant.lms.module.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProfileResponse", description = "Current-user profile view")
public class ProfileResponse {

    @Schema(description = "Current user ID", example = "385")
    private Integer userId;

    @Schema(description = "Display name", example = "Alex Rivera")
    private String displayName;

    @Schema(description = "Email", example = "regtest1@example.com")
    private String email;

    @Schema(description = "Role identifier", example = "USER")
    private String role;

    @Schema(description = "Level", example = "STUDENT")
    private String level;

    @Schema(description = "Avatar URL with cache-buster query, or null if none",
            example = "https://example.com/api/v2/users/385/avatar?v=abc")
    private String avatarUrl;

    @Schema(description = "Whether the user receives email notifications", example = "true")
    private Boolean emailNotifications;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
}
