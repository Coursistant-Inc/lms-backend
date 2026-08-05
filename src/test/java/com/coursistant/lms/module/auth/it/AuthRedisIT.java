package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.session.service.EmailVerificationService;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuthRedisIT extends AuthIntegrationTestBase {

    @Autowired
    private LoginGuardService loginGuardService;
    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    @Qualifier("generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Test
    void loginAttempts_haveTtlAndIsolateUserAdmin() {
        assertThrows(ApiException.class, () ->
                loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "a@ex.com", "BAD_PASSWORD"));
        Long ttl = generalRedisTemplate.getExpire("auth:login:user:attempts:a@ex.com", TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0, "attempts key must have TTL, got " + ttl);

        assertDoesNotThrow(() -> loginGuardService.assertNotLocked(LoginGuardService.ACCOUNT_ADMIN, "a@ex.com"));
    }

    @Test
    void fifthFailure_setsLockWithTtl() {
        for (int i = 0; i < 5; i++) {
            try {
                loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "lock@ex.com", "BAD_PASSWORD");
            } catch (ApiException ignored) {
            }
        }
        ApiException ex = assertThrows(ApiException.class,
                () -> loginGuardService.assertNotLocked(LoginGuardService.ACCOUNT_USER, "lock@ex.com"));
        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        Long ttl = generalRedisTemplate.getExpire("auth:login:user:lock:lock@ex.com", TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0, "lock key must have TTL, got " + ttl);
    }

    @Test
    void verification_luaConsumeOnce_andPurposeIsolation() {
        emailVerificationService.storeCode(EmailVerificationService.TYPE_REGISTER, "v@ex.com", "123456");
        assertEquals(EmailVerificationService.ConsumeResult.SUCCESS,
                emailVerificationService.consume(EmailVerificationService.TYPE_REGISTER, "v@ex.com", "123456"));
        assertEquals(EmailVerificationService.ConsumeResult.EXPIRED,
                emailVerificationService.consume(EmailVerificationService.TYPE_REGISTER, "v@ex.com", "123456"));

        emailVerificationService.storeCode(EmailVerificationService.TYPE_RESET, "v@ex.com", "654321");
        assertEquals(EmailVerificationService.ConsumeResult.EXPIRED,
                emailVerificationService.consume(EmailVerificationService.TYPE_REGISTER, "v@ex.com", "654321"));
        assertEquals(EmailVerificationService.ConsumeResult.SUCCESS,
                emailVerificationService.consume(EmailVerificationService.TYPE_RESET, "v@ex.com", "654321"));
    }

    @Test
    void concurrentConsume_onlyOneSuccess() throws Exception {
        emailVerificationService.storeCode(EmailVerificationService.TYPE_REGISTER, "c@ex.com", "111222");
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger ok = new AtomicInteger();
        Runnable r = () -> {
            try {
                start.await();
                if (emailVerificationService.consume(EmailVerificationService.TYPE_REGISTER, "c@ex.com", "111222")
                        == EmailVerificationService.ConsumeResult.SUCCESS) {
                    ok.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                done.countDown();
            }
        };
        new Thread(r).start();
        new Thread(r).start();
        start.countDown();
        done.await();
        assertEquals(1, ok.get());
    }

    @Test
    void attemptsExceeded_afterFiveWrongCodes() {
        emailVerificationService.storeCode(EmailVerificationService.TYPE_RESET, "w@ex.com", "999888");
        for (int i = 0; i < 5; i++) {
            emailVerificationService.consume(EmailVerificationService.TYPE_RESET, "w@ex.com", "000000");
        }
        assertEquals(EmailVerificationService.ConsumeResult.ATTEMPTS_EXCEEDED,
                emailVerificationService.consume(EmailVerificationService.TYPE_RESET, "w@ex.com", "999888"));
    }

    @Test
    void noPermanentAuthKeys_afterCleanup() {
        loginGuardService.clearOnSuccess(LoginGuardService.ACCOUNT_USER, "a@ex.com");
        List<String> bad = new ArrayList<>();
        for (String pattern : List.of("auth:login:*", "email:verification:*")) {
            var keys = generalRedisTemplate.keys(pattern);
            if (keys == null) {
                continue;
            }
            for (String k : keys) {
                Long ttl = generalRedisTemplate.getExpire(k, TimeUnit.SECONDS);
                if (ttl != null && ttl < 0) {
                    bad.add(k);
                }
            }
        }
        assertTrue(bad.isEmpty(), "permanent keys: " + bad);
    }
}
