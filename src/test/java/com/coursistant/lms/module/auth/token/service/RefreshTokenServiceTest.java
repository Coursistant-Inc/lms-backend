package com.coursistant.lms.module.auth.token.service;

import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.security.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private RedisTemplate<String, Object> refreshTokenRedisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpireDays", 30);
        lenient().when(refreshTokenRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getNewAccessToken_validToken_rotatesAndReturnsNew() {
        String oldToken = "old-refresh-token-uuid";
        RefreshToken dbToken = buildToken(1, "USER", oldToken, futureDate());

        when(valueOperations.get("refresh:used:" + oldToken)).thenReturn(null);
        when(refreshTokenMapper.selectByToken(oldToken)).thenReturn(dbToken);
        when(valueOperations.get("refresh:1:USER")).thenReturn(oldToken);

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.createAccessToken(1, "USER")).thenReturn("new-access-token");

            RefreshResult result = refreshTokenService.getNewAccessToken(oldToken);

            assertEquals("new-access-token", result.getAccessToken());
            assertNotNull(result.getRefreshToken());
            assertNotEquals(oldToken, result.getRefreshToken());

            verify(valueOperations).set(eq("refresh:used:" + oldToken), eq(result.getRefreshToken()), eq(Duration.ofSeconds(30)));
            verify(valueOperations).set(eq("refresh:1:USER"), eq(result.getRefreshToken()), eq(Duration.ofDays(30)));

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenMapper).updateById(captor.capture());
            assertEquals(result.getRefreshToken(), captor.getValue().getToken());
        }
    }

    @Test
    void getNewAccessToken_usedTokenInGraceWindow_returnsIdempotent() {
        String oldToken = "old-refresh-token-uuid";
        String mappedNew = "mapped-new-refresh-token";
        RefreshToken newDbToken = buildToken(1, "USER", mappedNew, futureDate());

        when(valueOperations.get("refresh:used:" + oldToken)).thenReturn(mappedNew);
        when(refreshTokenMapper.selectByToken(mappedNew)).thenReturn(newDbToken);

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.createAccessToken(1, "USER")).thenReturn("access-from-grace");

            RefreshResult result = refreshTokenService.getNewAccessToken(oldToken);

            assertEquals("access-from-grace", result.getAccessToken());
            assertEquals(mappedNew, result.getRefreshToken());
            verify(refreshTokenMapper, never()).updateById(any());
        }
    }

    @Test
    void getNewAccessToken_invalidToken_throwsException() {
        String badToken = "invalid-token";
        when(valueOperations.get("refresh:used:" + badToken)).thenReturn(null);
        when(refreshTokenMapper.selectByToken(badToken)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.getNewAccessToken(badToken));
        assertEquals(ErrorType.REFRESH_TOKEN_INVALID, ex.getErrorType());
    }

    @Test
    void validateRefreshToken_matchesRedis_returnsTrue() {
        String token = "valid-token";
        RefreshToken dbToken = buildToken(5, "ADMIN", token, futureDate());
        when(refreshTokenMapper.selectByToken(token)).thenReturn(dbToken);
        when(valueOperations.get("refresh:5:ADMIN")).thenReturn(token);

        assertTrue(refreshTokenService.validateRefreshToken(token));
    }

    @Test
    void validateRefreshToken_redisExpired_fallsBackToDb() {
        String token = "db-only-token";
        RefreshToken dbToken = buildToken(5, "USER", token, futureDate());
        when(refreshTokenMapper.selectByToken(token)).thenReturn(dbToken);
        when(valueOperations.get("refresh:5:USER")).thenReturn(null);

        assertTrue(refreshTokenService.validateRefreshToken(token));
        verify(refreshTokenMapper, never()).deleteById(any());
    }

    @Test
    void validateRefreshToken_fullyExpired_returnsFalse() {
        String token = "expired-token";
        RefreshToken dbToken = buildToken(5, "USER", token, pastDate());
        dbToken.setId(100);
        when(refreshTokenMapper.selectByToken(token)).thenReturn(dbToken);
        when(valueOperations.get("refresh:5:USER")).thenReturn(null);

        assertFalse(refreshTokenService.validateRefreshToken(token));
        verify(refreshTokenMapper).deleteById(100);
    }

    @Test
    void deleteByUserId_clearsRedisAndDb() {
        refreshTokenService.deleteByUserId(7, "USER");

        verify(refreshTokenMapper).deleteByUserId(7);
        verify(refreshTokenRedisTemplate).delete("refresh:7:USER");
    }

    private RefreshToken buildToken(Integer userId, String role, String token, Date expireTime) {
        RefreshToken rt = new RefreshToken();
        rt.setId(1);
        rt.setUserId(userId);
        rt.setRole(role);
        rt.setToken(token);
        rt.setExpireTime(expireTime);
        return rt;
    }

    private Date futureDate() {
        return new Date(System.currentTimeMillis() + 86_400_000L);
    }

    private Date pastDate() {
        return new Date(System.currentTimeMillis() - 86_400_000L);
    }
}
