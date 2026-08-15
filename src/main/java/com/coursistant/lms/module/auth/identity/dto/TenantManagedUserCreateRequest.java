package com.coursistant.lms.module.auth.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TenantManagedUserCreateRequest", description = "Create a user within the caller tenant")
public class TenantManagedUserCreateRequest {

    @Schema(description = "Email", example = "ta@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    public String email;

    @Schema(description = "Display name", example = "TA One", requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;

    @Schema(description = "Role (tenant-scoped)", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    public String role;

    @Schema(description = "Course level when role is USER", example = "TA")
    public String level;
}
