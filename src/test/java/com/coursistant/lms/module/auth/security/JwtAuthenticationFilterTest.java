package com.coursistant.lms.module.auth.security;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.AccessTokenAuthService;
import com.coursistant.lms.shared.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private AccessTokenAuthService accessTokenAuthService;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicPath_skipsAuth() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(accessTokenAuthService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(accessTokenAuthService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void protectedPath_setsSecurityContext() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(accessTokenAuthService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/users/me");
        request.setContextPath("/api");
        request.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(accessTokenAuthService.authenticateBearer(eq("Bearer abc"), eq(request)))
                .thenReturn(new AccessTokenAuthService.AuthenticatedPrincipal(10, "USER"));

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(10, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_writesUnauthorizedJson_andDoesNotContinue() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(accessTokenAuthService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/courses");
        request.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(accessTokenAuthService.authenticateBearer(isNull(), eq(request)))
                .thenThrow(new ApiException(ErrorType.INVALID_TOKEN));

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("INVALID_TOKEN"));
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
