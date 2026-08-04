package com.coursistant.lms.module.auth.unit;

import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginGuardServiceTest {

    @Mock
    private RedisTemplate<String, Object> generalRedisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LoginGuardService loginGuardService;

    @BeforeEach
    void setUp() {
        lenient().when(generalRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(generalRedisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
    }

    @Test
    void failures_1_to_4_doNotLock() {
        AtomicLong counter = new AtomicLong(0);
        when(valueOperations.increment(startsWith("auth:login:user:attempts:")))
                .thenAnswer(inv -> counter.incrementAndGet());

        for (int i = 1; i <= 4; i++) {
            ApiException ex = assertThrows(ApiException.class,
                    () -> loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "a@ex.com", "BAD_PASSWORD"));
            assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        }
        verify(valueOperations, never()).set(startsWith("auth:login:user:lock:"), any(), anyLong(), any());
        verify(generalRedisTemplate).expire(eq("auth:login:user:attempts:a@ex.com"), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    void fifthFailure_locksFifteenMinutes() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        ApiException ex = assertThrows(ApiException.class,
                () -> loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "a@ex.com", "BAD_PASSWORD"));
        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        verify(valueOperations).set("auth:login:user:lock:a@ex.com", "LOCKED", 15L, TimeUnit.MINUTES);
    }

    @Test
    void assertNotLocked_whenLocked_throwsSameCredentialsError() {
        when(generalRedisTemplate.hasKey("auth:login:user:lock:a@ex.com")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class,
                () -> loginGuardService.assertNotLocked(LoginGuardService.ACCOUNT_USER, "a@ex.com"));
        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        assertFalse(ex.getMessage().toLowerCase().contains("lock"));
        assertFalse(ex.getMessage().toLowerCase().contains("remaining"));
    }

    @Test
    void clearOnSuccess_deletesAttemptsAndLock() {
        loginGuardService.clearOnSuccess(LoginGuardService.ACCOUNT_USER, "a@ex.com");
        verify(generalRedisTemplate).delete("auth:login:user:attempts:a@ex.com");
        verify(generalRedisTemplate).delete("auth:login:user:lock:a@ex.com");
    }

    @Test
    void emailNormalized_beforeKeyLookup() {
        when(generalRedisTemplate.hasKey("auth:login:user:lock:a@ex.com")).thenReturn(false);
        assertDoesNotThrow(() -> loginGuardService.assertNotLocked(
                LoginGuardService.ACCOUNT_USER, "  A@Ex.Com  "));
        verify(generalRedisTemplate).hasKey("auth:login:user:lock:a@ex.com");
    }

    @Test
    void userAndAdmin_keySpacesIsolated() {
        when(valueOperations.increment("auth:login:user:attempts:a@ex.com")).thenReturn(5L);
        assertThrows(ApiException.class,
                () -> loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "a@ex.com", "BAD_PASSWORD"));
        verify(valueOperations).set("auth:login:user:lock:a@ex.com", "LOCKED", 15L, TimeUnit.MINUTES);
        verify(valueOperations, never()).set(eq("auth:login:admin:lock:a@ex.com"), any(), anyLong(), any());
    }

    @Test
    void redisFailure_onAssert_returns503() {
        when(generalRedisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        ApiException ex = assertThrows(ApiException.class,
                () -> loginGuardService.assertNotLocked(LoginGuardService.ACCOUNT_USER, "a@ex.com"));
        assertEquals(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE, ex.getErrorType());
    }

    @Test
    void redisFailure_onRecord_returns503_andDoesNotBypass() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("redis down"));
        ApiException ex = assertThrows(ApiException.class,
                () -> loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "a@ex.com", "BAD_PASSWORD"));
        assertEquals(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE, ex.getErrorType());
    }

    @Test
    void firstIncrement_setsAttemptsTtl() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        assertThrows(ApiException.class,
                () -> loginGuardService.recordFailure(LoginGuardService.ACCOUNT_ADMIN, "b@ex.com", "USER_NOT_FOUND"));
        verify(generalRedisTemplate).expire("auth:login:admin:attempts:b@ex.com", 15L, TimeUnit.MINUTES);
    }

    @Test
    void firstIncrement_expireFails_deletesKeyAndReturns503() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(generalRedisTemplate.expire(anyString(), eq(15L), eq(TimeUnit.MINUTES)))
                .thenThrow(new RuntimeException("expire down"));

        ApiException ex = assertThrows(ApiException.class,
                () -> loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "a@ex.com", "BAD_PASSWORD"));
        assertEquals(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE, ex.getErrorType());
        verify(generalRedisTemplate).delete("auth:login:user:attempts:a@ex.com");
    }

    @Test
    void firstIncrement_expireReturnsFalse_deletesKeyAndReturns503() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(generalRedisTemplate.expire(anyString(), eq(15L), eq(TimeUnit.MINUTES))).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "a@ex.com", "BAD_PASSWORD"));
        assertEquals(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE, ex.getErrorType());
        verify(generalRedisTemplate).delete("auth:login:user:attempts:a@ex.com");
    }

    @Test
    void authFailed_messageHasNoRemainingAttempts() {
        ApiException ex = loginGuardService.authFailed();
        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        assertEquals(ErrorType.INVALID_CREDENTIALS.getDefaultMessage(), ex.getMessage());
        assertFalse(ex.getMessage().matches("(?i).*remaining.*"));
    }
}
