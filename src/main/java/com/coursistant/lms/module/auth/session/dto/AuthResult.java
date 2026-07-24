package com.coursistant.lms.module.auth.session.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResult {
    private Integer userId;
    private String email;
    private String name;
    private String username;
    private String role;
    private String level;
    private String avatar;

    /** Only used by Controller to set the cookie; never serialized to JSON. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String refreshToken;

    private String accessToken;
}
