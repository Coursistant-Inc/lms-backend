package com.coursistant.lms.module.auth.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SystemManagedUserCreateRequest", description = "System-admin create of a managed user in a tenant")
public class SystemManagedUserCreateRequest {

    @Schema(description = "Email", example = "admin@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    public String email;

    @Schema(description = "Display name", example = "Tenant Admin", requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;

    @Schema(description = "Role", example = "TENANT_ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    public String role;

    @Schema(description = "Level when applicable", example = "NOT_APPLICABLE")
    public String level;

    @Schema(description = "Target tenant id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    public Integer tenantId;
}
