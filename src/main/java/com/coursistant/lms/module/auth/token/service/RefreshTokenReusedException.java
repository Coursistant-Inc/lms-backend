package com.coursistant.lms.module.auth.token.service;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;

/**
 * Thrown when a previous refresh token is presented outside the rotation grace window.
 * Must commit the device-session revoke (see {@code @Transactional(noRollbackFor=...)}).
 */
public class RefreshTokenReusedException extends ApiException {

    public RefreshTokenReusedException() {
        super(ErrorType.REFRESH_TOKEN_REUSED);
    }
}
