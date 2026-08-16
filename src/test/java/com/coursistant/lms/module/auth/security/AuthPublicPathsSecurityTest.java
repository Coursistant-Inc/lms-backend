package com.coursistant.lms.module.auth.security;

import com.coursistant.lms.shared.security.AuthPublicPaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthPublicPathsSecurityTest {

    private final AuthPublicPaths paths = new AuthPublicPaths();

    @AfterEach
    void resetDocsFlag() {
        paths.setDocsPublic(true);
    }

    @Test
    void postLogin_public_getLogin_notPublic() {
        assertTrue(AuthPublicPaths.isPublic("POST", "/api/v1/auth/login", "/api"));
        assertFalse(AuthPublicPaths.isPublic("GET", "/api/v1/auth/login", "/api"));
    }

    @Test
    void exactWhitelist_onlyListedPosts() {
        assertTrue(AuthPublicPaths.isPublic("POST", "/v1/auth/register", ""));
        assertTrue(AuthPublicPaths.isPublic("POST", "/v1/auth/password-resets", ""));
        assertFalse(AuthPublicPaths.isPublic("POST", "/v1/auth/password", ""));
        assertFalse(AuthPublicPaths.isPublic("POST", "/v1/auth/login/extra", ""));
        assertFalse(AuthPublicPaths.isPublic("POST", "/v1/auth/email-verifications/register/validate", ""));
    }

    @Test
    void getV1_health_public() {
        assertTrue(AuthPublicPaths.isPublic("GET", "/api/v1", "/api"));
        assertFalse(AuthPublicPaths.isPublic("POST", "/api/v1", "/api"));
    }

    @Test
    void getUserAvatar_public() {
        assertTrue(AuthPublicPaths.isPublic("GET", "/v2/users/1/avatar", ""));
        assertTrue(AuthPublicPaths.isPublic("GET", "/v2/users/385/avatar", ""));
        assertTrue(AuthPublicPaths.isPublic("GET", "/api/v2/users/385/avatar", "/api"));
        assertTrue(AuthPublicPaths.publicMethodPaths().contains("GET /v2/users/{userId}/avatar"));
        assertEquals(9, AuthPublicPaths.publicMethodPaths().size());
    }

    @Test
    void getUserAvatar_notPublicWhenNarrowed() {
        assertFalse(AuthPublicPaths.isPublic("GET", "/v2/users/1", ""));
        assertFalse(AuthPublicPaths.isPublic("GET", "/v2/users/1/avatar/extra", ""));
        assertFalse(AuthPublicPaths.isPublic("GET", "/v2/users/abc/avatar", ""));
        assertFalse(AuthPublicPaths.isPublic("PUT", "/v2/users/1/avatar", ""));
        assertFalse(AuthPublicPaths.isPublic("DELETE", "/v2/users/1/avatar", ""));
        assertFalse(AuthPublicPaths.isPublic("POST", "/v2/users/385/avatar", ""));
        assertFalse(AuthPublicPaths.isPublic("GET", "/files/x.png", ""));
        assertFalse(AuthPublicPaths.isPublic("GET", "/api/files/x.png", "/api"));
        assertFalse(AuthPublicPaths.isPublic("GET", "/v2/me/profile/avatar", ""));
    }

    @Test
    void docs_respectDocsPublicFlag() {
        paths.setDocsPublic(true);
        assertTrue(AuthPublicPaths.isPublic("GET", "/swagger-ui/index.html", ""));
        paths.setDocsPublic(false);
        assertFalse(AuthPublicPaths.isPublic("GET", "/swagger-ui/index.html", ""));
        assertFalse(AuthPublicPaths.isPublic("GET", "/v3/api-docs", ""));
    }

    @Test
    void contextPath_stripped() {
        assertTrue(AuthPublicPaths.isPublic("POST", "/api/v1/auth/logout", "/api"));
        assertFalse(AuthPublicPaths.isPublic("POST", "/api/v1/auth/logout", "/other"));
    }
}
