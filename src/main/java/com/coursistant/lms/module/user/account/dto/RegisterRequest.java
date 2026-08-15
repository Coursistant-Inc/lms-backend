package com.coursistant.lms.module.user.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public registration payload. Includes {@code tenantId} (required);
 * not part of shared {@code Account} to avoid polluting login bodies.
 */
@Schema(name = "RegisterRequest", description = "Public self-registration payload")
public class RegisterRequest {

    @Schema(description = "Email", example = "student@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Email verification code", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String verificationCode;

    @Schema(description = "Password", example = "Passw0rd1", format = "password",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Display name", example = "Student One", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Optional username", example = "student1")
    private String username;

    @Schema(description = "Tenant to join", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer tenantId;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }
}
