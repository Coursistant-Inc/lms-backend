package com.coursistant.lms.shared.security;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private AccessTokenAuthService accessTokenAuthService;

    @InjectMocks
    private JwtInterceptor jwtInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_delegatesToAccessTokenAuthService() {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        when(accessTokenAuthService.authenticateBearer(eq("Bearer valid.jwt.token"), eq(request)))
                .thenReturn(new AccessTokenAuthService.AuthenticatedPrincipal(10, "USER"));

        assertTrue(jwtInterceptor.preHandle(request, response, new Object()));
        verify(accessTokenAuthService).authenticateBearer("Bearer valid.jwt.token", request);
    }

    @Test
    void preHandle_skipsWhenSecurityContextAlreadyAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10, null, java.util.List.of()));
        request.setAttribute(AuthzService.ATTR_USER_ID, 10);
        request.setAttribute(AuthzService.ATTR_USER_ROLE, "USER");

        assertTrue(jwtInterceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(accessTokenAuthService);
    }

    @Test
    void preHandle_propagatesInvalidToken() {
        when(accessTokenAuthService.authenticateBearer(isNull(), eq(request)))
                .thenThrow(new ApiException(ErrorType.INVALID_TOKEN));

        ApiException ex = assertThrows(ApiException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }
}
