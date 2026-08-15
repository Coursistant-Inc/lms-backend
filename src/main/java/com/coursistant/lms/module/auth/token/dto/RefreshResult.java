package com.coursistant.lms.module.auth.token.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "RefreshResult", description = "Internal refresh outcome; HTTP APIs expose accessToken in body and refreshToken via Set-Cookie only")
public class RefreshResult {
    @Schema(description = "New JWT access token", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String accessToken;

    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY,
            description = "Rotated refresh token for cookie setting only; not returned in JSON body")
    private String refreshToken;
}
