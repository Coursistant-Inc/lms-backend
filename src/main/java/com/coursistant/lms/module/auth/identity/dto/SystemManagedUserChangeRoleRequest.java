package com.coursistant.lms.module.auth.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SystemManagedUserChangeRoleRequest", description = "System-admin change of managed user role/level")
public class SystemManagedUserChangeRoleRequest {

    @Schema(description = "New role", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    public String role;

    @Schema(description = "New level when applicable", example = "STUDENT")
    public String level;
}
