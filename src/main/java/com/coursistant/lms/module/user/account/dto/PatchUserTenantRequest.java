package com.coursistant.lms.module.user.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PatchUserTenantRequest", description = "Change the tenant a user belongs to")
public class PatchUserTenantRequest {

    @Schema(description = "Target tenant ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer tenantId;

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }
}
