package com.coursistant.lms.module.auth.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public login payload. Separate from {@code Account} so the entity is not the request schema.
 */
@Schema(name = "LoginRequest", description = "Credentials for POST /v1/auth/login")
public class LoginRequest {

    @Schema(description = "Account email", example = "student@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Account password", example = "Passw0rd1", format = "password",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Login table router role (USER, TENANT_ADMIN, SYSTEM_ADMIN, or legacy ADMIN)",
            example = "USER", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"USER", "TENANT_ADMIN", "SYSTEM_ADMIN", "ADMIN"})
    private String role;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
