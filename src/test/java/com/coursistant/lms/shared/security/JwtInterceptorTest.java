package com.coursistant.lms.shared.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private AdminService adminService;
    @Mock
    private UserService userService;
    @Mock
    private JwtParserUtil jwtParserUtil;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private DecodedJWT decodedJWT;
    @Mock
    private Claim userIdClaim;
    @Mock
    private Claim roleClaim;
    @Mock
    private Claim typeClaim;

    @InjectMocks
    private JwtInterceptor jwtInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(jwtInterceptor, "minIssuedAtConfig", "");
    }

    private void stubValidJwt(String role, int userId) {
        when(jwtParserUtil.verify("valid.jwt.token")).thenReturn(decodedJWT);
        when(decodedJWT.getClaim("userId")).thenReturn(userIdClaim);
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(decodedJWT.getClaim("type")).thenReturn(typeClaim);
        when(decodedJWT.getIssuedAt()).thenReturn(new Date());
        when(userIdClaim.asInt()).thenReturn(userId);
        when(roleClaim.asString()).thenReturn(role);
        when(typeClaim.asString()).thenReturn("access");
    }

    @Test
    void preHandle_validToken_setsAttributes() {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        stubValidJwt("USER", 10);
        when(stringRedisTemplate.hasKey("auth:principal:user:10")).thenReturn(false);
        when(userService.selectById(10)).thenReturn(new User());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean result = jwtInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(10, request.getAttribute("userId"));
        assertEquals("USER", request.getAttribute("userRole"));
        verify(userService).selectById(10);
        verify(valueOperations).set("auth:principal:user:10", "1", 5, TimeUnit.MINUTES);
    }

    @Test
    void preHandle_validToken_redisCacheHit_skipsDb() {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        stubValidJwt("USER", 10);
        when(stringRedisTemplate.hasKey("auth:principal:user:10")).thenReturn(true);

        boolean result = jwtInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(userService);
        verifyNoInteractions(adminService);
    }

    @Test
    void preHandle_systemAdmin_usesAdminCacheKey() {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        stubValidJwt("SYSTEM_ADMIN", 1);
        when(stringRedisTemplate.hasKey("auth:principal:admin:1")).thenReturn(false);
        when(adminService.selectById(1)).thenReturn(new Admin());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        assertTrue(jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals("SYSTEM_ADMIN", request.getAttribute("userRole"));
        verify(adminService).selectById(1);
        verify(valueOperations).set("auth:principal:admin:1", "1", 5, TimeUnit.MINUTES);
    }

    @Test
    void preHandle_legacyAdminRole_rejected() {
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        when(jwtParserUtil.verify("valid.jwt.token")).thenReturn(decodedJWT);
        when(decodedJWT.getClaim("userId")).thenReturn(userIdClaim);
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(decodedJWT.getClaim("type")).thenReturn(typeClaim);
        when(userIdClaim.asInt()).thenReturn(1);
        when(roleClaim.asString()).thenReturn("ADMIN");
        when(typeClaim.asString()).thenReturn("access");

        ApiException ex = assertThrows(ApiException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void preHandle_beforeCutover_rejected() {
        ReflectionTestUtils.setField(jwtInterceptor, "minIssuedAtConfig", "2099-01-01T00:00:00Z");
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        stubValidJwt("USER", 10);

        ApiException ex = assertThrows(ApiException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void preHandle_noAuthHeader_throwsTokenInvalid() {
        ApiException ex = assertThrows(ApiException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void preHandle_invalidBearerPrefix_throwsTokenInvalid() {
        request.addHeader("Authorization", "Token abc");

        ApiException ex = assertThrows(ApiException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }
}
