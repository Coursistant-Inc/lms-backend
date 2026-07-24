package com.coursistant.lms.module.auth.token.service;

import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
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
import java.util.List;

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
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpireDays", 14);
        lenient().when(refreshTokenRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getNewAccessToken_validToken_rotatesAndReturnsNew() {
        String oldToken = "old-refresh-token-uuid";
        RefreshToken dbToken = buildToken(1, "USER", oldToken, futureDate());

        when(valueOperations.get("refresh:used:" + oldToken)).thenReturn(null);
        when(refreshTokenRedisTemplate.hasKey("refresh:token:" + oldToken)).thenReturn(true);
        when(refreshTokenMapper.selectByToken(oldToken)).thenReturn(dbToken);

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.createAccessToken(1, "USER")).thenReturn("new-access-token");

            RefreshResult result = refreshTokenService.getNewAccessToken(oldToken);

            assertEquals("new-access-token", result.getAccessToken());
            assertNotNull(result.getRefreshToken());
            assertNotEquals(oldToken, result.getRefreshToken());

            verify(valueOperations).set(eq("refresh:used:" + oldToken), eq(result.getRefreshToken()), eq(Duration.ofSeconds(30)));
            verify(refreshTokenRedisTemplate).delete("refresh:token:" + oldToken);
            verify(valueOperations).set(eq("refresh:token:" + result.getRefreshToken()), eq("1:USER"), eq(Duration.ofDays(14)));

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
        when(refreshTokenRedisTemplate.hasKey("refresh:token:" + badToken)).thenReturn(false);
        when(refreshTokenMapper.selectByToken(badToken)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.getNewAccessToken(badToken));
        assertEquals(ErrorType.REFRESH_TOKEN_INVALID, ex.getErrorType());
    }

    @Test
    void validateRefreshToken_matchesRedis_returnsTrue() {
        String token = "valid-token";
        when(refreshTokenRedisTemplate.hasKey("refresh:token:" + token)).thenReturn(true);

        assertTrue(refreshTokenService.validateRefreshToken(token));
        verify(refreshTokenMapper, never()).selectByToken(any());
    }

    @Test
    void validateRefreshToken_redisExpired_fallsBackToDb() {
        String token = "db-only-token";
        RefreshToken dbToken = buildToken(5, "USER", token, futureDate());
        when(refreshTokenRedisTemplate.hasKey("refresh:token:" + token)).thenReturn(false);
        when(refreshTokenMapper.selectByToken(token)).thenReturn(dbToken);

        assertTrue(refreshTokenService.validateRefreshToken(token));
        verify(refreshTokenMapper, never()).deleteById(any());
    }

    @Test
    void validateRefreshToken_fullyExpired_returnsFalse() {
        String token = "expired-token";
        RefreshToken dbToken = buildToken(5, "USER", token, pastDate());
        dbToken.setId(100);
        when(refreshTokenRedisTemplate.hasKey("refresh:token:" + token)).thenReturn(false);
        when(refreshTokenMapper.selectByToken(token)).thenReturn(dbToken);

        assertFalse(refreshTokenService.validateRefreshToken(token));
        verify(refreshTokenMapper).deleteById(100);
    }

    @Test
    void deleteByUserId_clearsDbThenRedis() {
        RefreshToken t1 = buildToken(7, "USER", "tok-a", futureDate());
        RefreshToken t2 = buildToken(7, "USER", "tok-b", futureDate());
        when(refreshTokenMapper.selectAllByUserId(7)).thenReturn(List.of(t1, t2));

        refreshTokenService.deleteByUserId(7, "USER");

        verify(refreshTokenMapper).deleteByUserId(7);
        verify(refreshTokenRedisTemplate).delete("refresh:token:tok-a");
        verify(refreshTokenRedisTemplate).delete("refresh:token:tok-b");
    }

    @Test
    void deleteByToken_clearsDbThenRedis() {
        refreshTokenService.deleteByToken("device-token");

        verify(refreshTokenMapper).deleteByToken("device-token");
        verify(refreshTokenRedisTemplate).delete("refresh:token:device-token");
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
