package com.coursistant.lms.shared.security;

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Blocks business APIs when {@code mustChangePassword=true}.
 * Only {@code PUT /v1/auth/password} is exempt (after context-path strip).
 */
@Component
public class PasswordChangeGuard {

    public void assertMayProceed(User user, HttpServletRequest request) {
        if (user == null || !Boolean.TRUE.equals(user.getMustChangePassword())) {
            return;
        }
        if (isPasswordChangeRequest(request)) {
            return;
        }
        throw new ApiException(ErrorType.PASSWORD_CHANGE_REQUIRED);
    }

    static boolean isPasswordChangeRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String path = AuthPublicPaths.stripContext(request.getRequestURI(), request.getContextPath());
        return HttpMethod.PUT.matches(request.getMethod()) && "/v1/auth/password".equals(path);
    }
}
