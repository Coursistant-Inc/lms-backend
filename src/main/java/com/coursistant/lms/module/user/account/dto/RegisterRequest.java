package com.coursistant.lms.module.user.account.dto;

/**
 * Public registration payload. Includes {@code tenantId} (required);
 * not part of shared {@code Account} to avoid polluting login bodies.
 */
public class RegisterRequest {

    private String email;
    private String password;
    private String name;
    private String username;
    private Integer tenantId;

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
