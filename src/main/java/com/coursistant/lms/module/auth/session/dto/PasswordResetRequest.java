package com.coursistant.lms.module.auth.session.dto;

import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public password reset after email verification code consumption.
 */
@Schema(name = "PasswordResetRequest", description = "Reset password with email verification code")
public class PasswordResetRequest {

    @Schema(description = "Account email", example = "student@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Email verification code", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String verificationCode;

    @Schema(description = "New password", example = "NewPassw0rd1", format = "password",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = AccountIdentityService.normalizeEmail(email);
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
