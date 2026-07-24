package com.coursistant.lms.module.auth.session.controller;

import com.coursistant.lms.module.auth.session.controller.WebController;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.chat.service.CoursistanceService;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.module.user.service.UserService;
import com.coursistant.lms.shared.api.ApiExceptionHandler;
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
    private CoursistanceService coursistanceService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

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
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=new-refresh-token"));
        assertTrue(setCookie.toLowerCase().contains("httponly"));
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
}
