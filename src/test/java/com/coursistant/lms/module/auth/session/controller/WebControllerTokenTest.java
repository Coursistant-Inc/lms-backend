package com.coursistant.lms.module.auth.session.controller;

import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.auth.session.dto.AuthResult;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiExceptionHandler;
import com.coursistant.lms.shared.security.AuthzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WebControllerTokenTest {

    @Mock
    private AdminService adminService;
    @Mock
    private UserService userService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AuthzService authzService;

    @InjectMocks
    private WebController webController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(webController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void refreshToken_validCookie_returnsNewAccessTokenAndSetsCookie() throws Exception {
        when(valueOperations.increment(startsWith("ratelimit:refresh:"))).thenReturn(1L);
        when(refreshTokenService.getNewAccessToken("old-refresh"))
                .thenReturn(new RefreshResult("new-access-token", "new-refresh-token"));

        MvcResult result = mockMvc.perform(post("/v1/auth/refresh-token")
                        .cookie(new Cookie("refreshToken", "old-refresh"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("new-access-token"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("refresh"))))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("new-refresh-token"));
        assertFalse(body.contains("old-refresh"));

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=new-refresh-token"));
        assertTrue(setCookie.toLowerCase().contains("httponly"));
        assertTrue(setCookie.toLowerCase().contains("secure"));
        assertTrue(setCookie.contains("Path=/") || setCookie.toLowerCase().contains("path=/"));
        assertTrue(setCookie.contains("SameSite=Lax") || setCookie.contains("SameSite=Lax".toLowerCase())
                || setCookie.toLowerCase().contains("samesite=lax"));
        verify(stringRedisTemplate).expire(startsWith("ratelimit:refresh:"), eq(60L), any());
    }

    @Test
    void refreshToken_noCookie_returns401() throws Exception {
        when(valueOperations.increment(startsWith("ratelimit:refresh:"))).thenReturn(1L);

        mockMvc.perform(post("/v1/auth/refresh-token").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void refreshToken_rateLimitExceeded_returns429() throws Exception {
        when(valueOperations.increment(startsWith("ratelimit:refresh:"))).thenReturn(11L);

        mockMvc.perform(post("/v1/auth/refresh-token")
                        .cookie(new Cookie("refreshToken", "any"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("Too many requests, please try again later"));

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logout_validAuth_clearsTokenAndCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "current-device-token"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andReturn();

        verify(refreshTokenService).deleteByToken("current-device-token");

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken="));
        assertTrue(setCookie.contains("Max-Age=0") || setCookie.toLowerCase().contains("max-age=0"));
    }

    @Test
    void login_jsonBody_omitsRefreshToken_andSetsSecureCookie() throws Exception {
        AuthResult auth = new AuthResult();
        auth.setUserId(21);
        auth.setEmail("student@example.com");
        auth.setRole("USER");
        auth.setAccessToken("access-abc");
        auth.setRefreshToken("refresh-secret-xyz");
        when(userService.login(any(Account.class))).thenReturn(auth);

        String bodyJson = "{\"email\":\"student@example.com\",\"password\":\"Passw0rd1\",\"role\":\"USER\"}";

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-abc"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("refresh-secret-xyz"));

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=refresh-secret-xyz"));
        assertTrue(setCookie.toLowerCase().contains("httponly"));
        assertTrue(setCookie.toLowerCase().contains("secure"));
        assertTrue(setCookie.toLowerCase().contains("samesite=lax"));
    }
}
