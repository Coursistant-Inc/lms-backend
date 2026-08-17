package com.coursistant.lms.module.interaction.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "DigestRunRequest", description = "Manual daily digest run (SYSTEM_ADMIN)")
public class DigestRunRequest {

    @Schema(description = "Digest calendar date (required)", example = "2026-08-17",
            requiredMode = Schema.RequiredMode.REQUIRED, format = "date")
    private LocalDate digestDate;

    @Schema(description = "Optional tenant scope. Omit or null to run all tenants.", example = "1")
    private Integer tenantId;

    public LocalDate getDigestDate() {
        return digestDate;
    }

    public void setDigestDate(LocalDate digestDate) {
        this.digestDate = digestDate;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }
}
