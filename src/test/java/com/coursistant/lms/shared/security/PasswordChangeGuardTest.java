package com.coursistant.lms.shared.security;

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class PasswordChangeGuardTest {

    private final PasswordChangeGuard guard = new PasswordChangeGuard();

    @Test
    void allowsWhenFlagFalseOrNull() {
        User user = new User();
        user.setMustChangePassword(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/courses");
        request.setContextPath("/api");
        assertDoesNotThrow(() -> guard.assertMayProceed(user, request));
        assertDoesNotThrow(() -> guard.assertMayProceed(null, request));
        user.setMustChangePassword(null);
        assertDoesNotThrow(() -> guard.assertMayProceed(user, request));
    }

    @Test
    void blocksBusinessApiWhenMustChangePassword() {
        User user = new User();
        user.setMustChangePassword(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/courses");
        request.setContextPath("/api");
        ApiException ex = assertThrows(ApiException.class, () -> guard.assertMayProceed(user, request));
        assertEquals(ErrorType.PASSWORD_CHANGE_REQUIRED, ex.getErrorType());
    }

    @Test
    void allowsPutPasswordWithContextPath() {
        User user = new User();
        user.setMustChangePassword(true);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/auth/password");
        request.setContextPath("/api");
        assertDoesNotThrow(() -> guard.assertMayProceed(user, request));
    }

    @Test
    void doesNotAllowPostPassword() {
        User user = new User();
        user.setMustChangePassword(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/password");
        request.setContextPath("/api");
        ApiException ex = assertThrows(ApiException.class, () -> guard.assertMayProceed(user, request));
        assertEquals(ErrorType.PASSWORD_CHANGE_REQUIRED, ex.getErrorType());
    }
}
