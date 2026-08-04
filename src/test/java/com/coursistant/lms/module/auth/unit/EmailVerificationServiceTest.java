package com.coursistant.lms.module.auth.unit;

import com.coursistant.lms.module.auth.session.service.EmailVerificationService;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
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
import org.springframework.data.redis.core.script.RedisScript;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private LoginGuardService loginGuardService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(loginGuardService.redisUnavailable(any()))
                .thenReturn(new ApiException(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE));
    }

    @Test
    void storeCode_usesTenMinuteTtlAndNormalizedEmail() {
        emailVerificationService.storeCode(EmailVerificationService.TYPE_REGISTER, "  A@Ex.Com ", "012345");
        verify(valueOperations).set("email:verification:register:a@ex.com", "012345", 10, TimeUnit.MINUTES);
    }

    @Test
    void assertCanSend_cooldownBlocks() {
        when(stringRedisTemplate.hasKey("email:verification:cooldown:register:a@ex.com")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.assertCanSend(EmailVerificationService.TYPE_REGISTER, "a@ex.com"));
        assertEquals(ErrorType.VERIFICATION_RESEND_COOLDOWN, ex.getErrorType());
    }

    @Test
    void assertCanSend_hourlyLimitBlocksSixth() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get("email:verification:hourly:reset:a@ex.com")).thenReturn("5");
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.assertCanSend(EmailVerificationService.TYPE_RESET, "a@ex.com"));
        assertEquals(ErrorType.VERIFICATION_HOURLY_LIMIT, ex.getErrorType());
    }

    @Test
    void assertCanSend_firstSend_setsCooldownAndHourly() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);
        emailVerificationService.assertCanSend(EmailVerificationService.TYPE_REGISTER, "a@ex.com");
        verify(valueOperations).set(eq("email:verification:cooldown:register:a@ex.com"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("email:verification:hourly:register:a@ex.com"), eq("1"), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void consume_mapsLuaResults() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenReturn("SUCCESS", "INVALID", "EXPIRED", "ATTEMPTS_EXCEEDED");
        assertEquals(EmailVerificationService.ConsumeResult.SUCCESS,
                emailVerificationService.consume("register", "a@ex.com", "123456"));
        assertEquals(EmailVerificationService.ConsumeResult.INVALID,
                emailVerificationService.consume("register", "a@ex.com", "123456"));
        assertEquals(EmailVerificationService.ConsumeResult.EXPIRED,
                emailVerificationService.consume("register", "a@ex.com", "123456"));
        assertEquals(EmailVerificationService.ConsumeResult.ATTEMPTS_EXCEEDED,
                emailVerificationService.consume("register", "a@ex.com", "123456"));
    }

    @Test
    void requireConsumeSuccess_mapsErrorTypes() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenReturn("EXPIRED");
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.requireConsumeSuccess("reset", "a@ex.com", "111111"));
        assertEquals(ErrorType.VERIFICATION_CODE_EXPIRED, ex.getErrorType());
    }

    @Test
    void requireConsumeSuccess_attemptsExceeded() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenReturn("ATTEMPTS_EXCEEDED");
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.requireConsumeSuccess("reset", "a@ex.com", "111111"));
        assertEquals(ErrorType.VERIFICATION_ATTEMPTS_EXCEEDED, ex.getErrorType());
    }

    @Test
    void blankCode_returnsInvalidWithoutRedis() {
        assertEquals(EmailVerificationService.ConsumeResult.INVALID,
                emailVerificationService.consume("register", "a@ex.com", " "));
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), any(), any(), any());
    }

    @Test
    void invalidType_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.storeCode("invite", "a@ex.com", "000000"));
        assertEquals(ErrorType.BAD_REQUEST, ex.getErrorType());
    }

    @Test
    void redisFailure_onStore_returns503() {
        doThrow(new RuntimeException("down")).when(valueOperations).set(anyString(), anyString(), anyLong(), any());
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.storeCode("register", "a@ex.com", "000001"));
        assertEquals(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE, ex.getErrorType());
    }

    @Test
    void leadingZeroCode_acceptedAsSixDigits() {
        emailVerificationService.storeCode("register", "a@ex.com", "000042");
        verify(valueOperations).set(eq("email:verification:register:a@ex.com"), eq("000042"), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void registerAndReset_useIsolatedKeys() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);

        emailVerificationService.assertCanSend(EmailVerificationService.TYPE_REGISTER, "a@ex.com");
        emailVerificationService.assertCanSend(EmailVerificationService.TYPE_RESET, "a@ex.com");

        verify(valueOperations).set(eq("email:verification:cooldown:register:a@ex.com"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("email:verification:cooldown:reset:a@ex.com"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("email:verification:hourly:register:a@ex.com"), eq("1"), eq(1L), eq(TimeUnit.HOURS));
        verify(valueOperations).set(eq("email:verification:hourly:reset:a@ex.com"), eq("1"), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void consume_concurrentOnlyOneSuccess() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenReturn("SUCCESS", "EXPIRED");
        assertEquals(EmailVerificationService.ConsumeResult.SUCCESS,
                emailVerificationService.consume("register", "a@ex.com", "123456"));
        assertEquals(EmailVerificationService.ConsumeResult.EXPIRED,
                emailVerificationService.consume("register", "a@ex.com", "123456"));
    }

    @Test
    void redisFailure_onConsume_returns503() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenThrow(new RuntimeException("lua down"));
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.requireConsumeSuccess("register", "a@ex.com", "123456"));
        assertEquals(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE, ex.getErrorType());
    }

    @Test
    void assertCanSend_incrementsExistingHourlyCounter() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get("email:verification:hourly:register:a@ex.com")).thenReturn("2");
        when(valueOperations.increment("email:verification:hourly:register:a@ex.com")).thenReturn(3L);

        emailVerificationService.assertCanSend(EmailVerificationService.TYPE_REGISTER, "a@ex.com");

        verify(valueOperations).set(eq("email:verification:cooldown:register:a@ex.com"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        verify(valueOperations).increment("email:verification:hourly:register:a@ex.com");
    }

    @Test
    void requireConsumeSuccess_invalidCode() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenReturn("INVALID");
        ApiException ex = assertThrows(ApiException.class,
                () -> emailVerificationService.requireConsumeSuccess("register", "a@ex.com", "000000"));
        assertEquals(ErrorType.INVALID_VERIFICATION_CODE, ex.getErrorType());
    }

    @Test
    void consume_nullLuaResult_mapsToExpired() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenReturn(null);
        assertEquals(EmailVerificationService.ConsumeResult.EXPIRED,
                emailVerificationService.consume("reset", "a@ex.com", "123456"));
    }

    @Test
    void assertCanSend_fifthHourlyAllowed_sixthBlocked() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get("email:verification:hourly:register:a@ex.com")).thenReturn("4");
        when(valueOperations.increment("email:verification:hourly:register:a@ex.com")).thenReturn(5L);
        assertDoesNotThrow(() -> emailVerificationService.assertCanSend(
                EmailVerificationService.TYPE_REGISTER, "a@ex.com"));

        when(valueOperations.get("email:verification:hourly:register:a@ex.com")).thenReturn("5");
        ApiException ex = assertThrows(ApiException.class, () -> emailVerificationService.assertCanSend(
                EmailVerificationService.TYPE_REGISTER, "a@ex.com"));
        assertEquals(ErrorType.VERIFICATION_HOURLY_LIMIT, ex.getErrorType());
    }
}
