package com.coursistant.lms.shared.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.shared.security.JwtInterceptor;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.user.entity.User;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.service.UserService;
import com.coursistant.lms.shared.security.JwtParserUtil;
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

    @InjectMocks
    private JwtInterceptor jwtInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void preHandle_validToken_setsAttributes() {
        request.setRequestURI("/api/courses");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtParserUtil.verify("valid.jwt.token")).thenReturn(decodedJWT);
        when(decodedJWT.getClaim("userId")).thenReturn(userIdClaim);
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(userIdClaim.asInt()).thenReturn(10);
        when(roleClaim.asString()).thenReturn("USER");
        when(stringRedisTemplate.hasKey("user:active:10")).thenReturn(false);
        when(userService.selectById(10)).thenReturn(new User());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean result = jwtInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(10, request.getAttribute("userId"));
        assertEquals("USER", request.getAttribute("userRole"));
        verify(userService).selectById(10);
        verify(valueOperations).set("user:active:10", "1", 5, TimeUnit.MINUTES);
    }

    @Test
    void preHandle_validToken_redisCacheHit_skipsDb() {
        request.setRequestURI("/api/courses");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtParserUtil.verify("valid.jwt.token")).thenReturn(decodedJWT);
        when(decodedJWT.getClaim("userId")).thenReturn(userIdClaim);
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(userIdClaim.asInt()).thenReturn(10);
        when(roleClaim.asString()).thenReturn("USER");
        when(stringRedisTemplate.hasKey("user:active:10")).thenReturn(true);

        boolean result = jwtInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(10, request.getAttribute("userId"));
        assertEquals("USER", request.getAttribute("userRole"));
        verifyNoInteractions(userService);
        verifyNoInteractions(adminService);
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void preHandle_noAuthHeader_throwsTokenInvalid() {
        request.setRequestURI("/api/courses");

        CustomException ex = assertThrows(CustomException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ResultCodeEnum.TOKEN_INVALID_ERROR.code, ex.getCode());
    }

    @Test
    void preHandle_invalidBearerPrefix_throwsTokenInvalid() {
        request.setRequestURI("/api/courses");
        request.addHeader("Authorization", "Token abc");

        CustomException ex = assertThrows(CustomException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ResultCodeEnum.TOKEN_INVALID_ERROR.code, ex.getCode());
    }

    @Test
    void preHandle_userNotFound_throwsUserNotExist() {
        request.setRequestURI("/api/courses");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtParserUtil.verify("valid.jwt.token")).thenReturn(decodedJWT);
        when(decodedJWT.getClaim("userId")).thenReturn(userIdClaim);
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(userIdClaim.asInt()).thenReturn(99);
        when(roleClaim.asString()).thenReturn("USER");
        when(stringRedisTemplate.hasKey("user:active:99")).thenReturn(false);
        when(userService.selectById(99)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> jwtInterceptor.preHandle(request, response, new Object()));
        assertEquals(ResultCodeEnum.USER_NOT_EXIST_ERROR.code, ex.getCode());
    }

    @Test
    void preHandle_rocketchatPath_skipsAuth() {
        request.setRequestURI("/api/rocketchat/login");

        boolean result = jwtInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verifyNoInteractions(jwtParserUtil);
        verifyNoInteractions(stringRedisTemplate);
    }
}
