package com.coursistant.lms.module.auth.session.dto;

import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;

/**
 * Public password reset after email verification code consumption.
 */
public class PasswordResetRequest {

    private String email;
    private String verificationCode;
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
