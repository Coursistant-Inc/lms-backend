package com.coursistant.lms.module.auth.session.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "AuthResult", description = "Login/register result; refresh token is cookie-only and omitted from JSON")
public class AuthResult {
    @Schema(description = "Principal id", example = "21")
    private Integer userId;
    @Schema(description = "Email", example = "student@example.com")
    private String email;
    @Schema(description = "Display name", example = "Student One")
    private String name;
    @Schema(description = "Username", example = "student1")
    private String username;
    @Schema(description = "Authorization role from DB", example = "USER")
    private String role;
    @Schema(description = "Level when applicable", example = "STUDENT")
    private String level;
    @Schema(description = "Avatar URL or object key", example = "avatars/21.png")
    private String avatar;

    /** Only used by Controller to set the cookie; never serialized to JSON. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY,
            description = "Refresh token for cookie setting only; never present in response JSON")
    private String refreshToken;

    @Schema(description = "JWT access token for Authorization: Bearer", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Whether the principal must change password before other APIs", example = "false")
    private Boolean mustChangePassword;
}
