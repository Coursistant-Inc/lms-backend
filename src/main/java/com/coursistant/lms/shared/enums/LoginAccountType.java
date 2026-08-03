package com.coursistant.lms.shared.enums;

/**
 * Login request account-table router only - not an authorization role.
 * Legacy ADMIN maps to the admin table for frontend compatibility.
 */
public enum LoginAccountType {
    SYSTEM_ADMIN,
    USER,
    TENANT_ADMIN,
    /** Transitional login body value; resolves to admin table */
    ADMIN;

    public static LoginAccountType fromRequestRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        for (LoginAccountType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    public boolean routesToAdminTable() {
        return this == SYSTEM_ADMIN || this == ADMIN;
    }

    public boolean routesToUserTable() {
        return this == USER || this == TENANT_ADMIN;
    }
}
