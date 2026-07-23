package com.coursistant.lms.module.auth.token.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshResult {
    private String accessToken;
    private String refreshToken;
}
