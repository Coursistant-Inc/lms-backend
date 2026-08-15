package com.coursistant.lms.module.auth.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TenantManagedUserChangeRoleRequest", description = "Change role/level of a tenant-managed user")
public class TenantManagedUserChangeRoleRequest {

    @Schema(description = "New role", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    public String role;

    @Schema(description = "New level when applicable", example = "STUDENT")
    public String level;
}
