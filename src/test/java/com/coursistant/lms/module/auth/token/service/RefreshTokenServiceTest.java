package com.coursistant.lms.module.auth.token.service;

import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.repository.UserMapper;
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

    @Mock
    private UserMapper userMapper;

    @Mock
    private AdminMapper adminMapper;

    @Mock
    private TenantMapper tenantMapper;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpireDays", 14);
        ReflectionTestUtils.setField(refreshTokenService, "refreshRotationGraceSeconds", 30);
        ReflectionTestUtils.setField(refreshTokenService, "clock", java.time.Clock.systemUTC());
        lenient().when(refreshTokenRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(refreshTokenRedisTemplate.hasKey(anyString())).thenReturn(false);
    }

    @Test
    void getNewAccessToken_currentToken_rotatesWithPreviousGrace() {
        String oldToken = "old-refresh-token-uuid";
        RefreshToken dbToken = buildToken(1, "USER", oldToken, futureDate());
        dbToken.setSessionId("sess1");

        when(valueOperations.get("refresh:token:" + oldToken)).thenReturn("sess1:1:USER");
        when(refreshTokenMapper.selectByToken(oldToken)).thenReturn(dbToken);
        when(refreshTokenMapper.selectBySessionIdForUpdate("sess1")).thenReturn(dbToken);

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.createAccessToken(1, "USER", 1, 1)).thenReturn("new-access-token");

            RefreshResult result = refreshTokenService.getNewAccessToken(oldToken);

            assertEquals("new-access-token", result.getAccessToken());
            assertNotNull(result.getRefreshToken());
            assertNotEquals(oldToken, result.getRefreshToken());

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenMapper).updateById(captor.capture());
            assertEquals(result.getRefreshToken(), captor.getValue().getToken());
            assertEquals(oldToken, captor.getValue().getPreviousToken());
            assertNotNull(captor.getValue().getPreviousValidUntil());
        }
    }

    @Test
    void getNewAccessToken_previousTokenInGrace_returnsSameCurrent() {
        String previous = "previous-token";
        String current = "current-token";
        RefreshToken locked = buildToken(1, "USER", current, futureDate());
        locked.setSessionId("sess1");
        locked.setPreviousToken(previous);
        locked.setPreviousValidUntil(new Date(System.currentTimeMillis() + 20_000L));

        when(valueOperations.get("refresh:token:" + previous)).thenReturn(null);
        when(refreshTokenMapper.selectByToken(previous)).thenReturn(locked);
        when(refreshTokenMapper.selectBySessionIdForUpdate("sess1")).thenReturn(locked);

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.createAccessToken(1, "USER", 1, 1)).thenReturn("access-from-grace");

            RefreshResult result = refreshTokenService.getNewAccessToken(previous);

            assertEquals("access-from-grace", result.getAccessToken());
            assertEquals(current, result.getRefreshToken());
            verify(refreshTokenMapper, never()).updateById(any());
            verify(refreshTokenMapper, never()).deleteBySessionId(any());
        }
    }

    @Test
    void getNewAccessToken_previousTokenOutsideGrace_revokesDeviceOnly() {
        String previous = "previous-token";
        String current = "current-token";
        RefreshToken locked = buildToken(1, "USER", current, futureDate());
        locked.setSessionId("sess1");
        locked.setPreviousToken(previous);
        locked.setPreviousValidUntil(new Date(System.currentTimeMillis() - 1_000L));

        when(refreshTokenMapper.selectByToken(previous)).thenReturn(locked);
        when(refreshTokenMapper.selectBySessionIdForUpdate("sess1")).thenReturn(locked);

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.getNewAccessToken(previous));
        assertEquals(ErrorType.REFRESH_TOKEN_REUSED, ex.getErrorType());
        verify(refreshTokenMapper).deleteBySessionId("sess1");
    }

    @Test
    void getNewAccessToken_invalidToken_throwsException() {
        String badToken = "invalid-token";
        when(valueOperations.get("refresh:token:" + badToken)).thenReturn(null);
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
    void deleteByUserId_clearsDbThenRedis() {
        RefreshToken t1 = buildToken(7, "USER", "tok-a", futureDate());
        t1.setSessionId("s1");
        RefreshToken t2 = buildToken(7, "USER", "tok-b", futureDate());
        t2.setSessionId("s2");
        when(refreshTokenMapper.selectAllByUserId(7)).thenReturn(List.of(t1, t2));

        refreshTokenService.deleteByUserId(7, "USER");

        verify(refreshTokenMapper).deleteByUserId(7);
        verify(refreshTokenRedisTemplate).delete("refresh:token:tok-a");
        verify(refreshTokenRedisTemplate).delete("refresh:token:tok-b");
    }

    @Test
    void deleteByToken_clearsDbThenRedis() {
        when(refreshTokenMapper.selectByToken("device-token")).thenReturn(null);
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
}
