package com.coursistant.lms.module.auth.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Logged-in password change. Identity comes from the authenticated principal, not the body.
 */
@Schema(name = "ChangePasswordRequest", description = "Change password for the authenticated principal")
public class ChangePasswordRequest {

    @Schema(description = "Current password", example = "OldPassw0rd", format = "password",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @Schema(description = "New password", example = "NewPassw0rd1", format = "password",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
