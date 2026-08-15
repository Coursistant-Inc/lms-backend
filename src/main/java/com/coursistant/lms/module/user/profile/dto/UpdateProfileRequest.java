package com.coursistant.lms.module.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateProfileRequest", description = "Partial profile update; at least one field required")
public class UpdateProfileRequest {

    @Schema(description = "Display name (1–100 chars after trim)", example = "Alex Rivera", maxLength = 100)
    private String displayName;

    @Schema(description = "Whether to receive email notifications", example = "false")
    private Boolean emailNotifications;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
}
