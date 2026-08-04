package com.coursistant.lms.module.auth.session.service;

import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Unified login lockout for user/admin tables.
 * Failures always surface as INVALID_CREDENTIALS to callers (anti-enumeration).
 */
@Service
public class LoginGuardService {

    private static final Logger log = LoggerFactory.getLogger(LoginGuardService.class);

    public static final String ACCOUNT_USER = "user";
    public static final String ACCOUNT_ADMIN = "admin";

    private static final int MAX_FAILURES = 5;
    private static final long ATTEMPTS_TTL_MINUTES = 15;
    private static final long LOCK_TTL_MINUTES = 15;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    public void assertNotLocked(String accountTable, String email) {
        String normalized = AccountIdentityService.normalizeEmail(email);
        String lockKey = lockKey(accountTable, normalized);
        try {
            if (Boolean.TRUE.equals(generalRedisTemplate.hasKey(lockKey))) {
                log.info("Login rejected: locked accountTable={} emailHash={}", accountTable, hashEmail(normalized));
                throw authFailed();
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw redisUnavailable(e);
        }
    }

    public void recordFailure(String accountTable, String email, String reason) {
        String normalized = AccountIdentityService.normalizeEmail(email);
        String attemptsKey = attemptsKey(accountTable, normalized);
        String lockKey = lockKey(accountTable, normalized);
        try {
            Long count = generalRedisTemplate.opsForValue().increment(attemptsKey);
            if (count != null && count == 1L) {
                Boolean expired;
                try {
                    expired = generalRedisTemplate.expire(attemptsKey, ATTEMPTS_TTL_MINUTES, TimeUnit.MINUTES);
                } catch (Exception expireEx) {
                    try {
                        generalRedisTemplate.delete(attemptsKey);
                    } catch (Exception ignored) {
                        // best-effort cleanup to avoid permanent keys without TTL
                    }
                    throw expireEx;
                }
                if (!Boolean.TRUE.equals(expired)) {
                    generalRedisTemplate.delete(attemptsKey);
                    throw new IllegalStateException("Failed to set TTL on login attempts key");
                }
            }
            log.info("Login failure reason={} accountTable={} emailHash={} attempts={}",
                    reason, accountTable, hashEmail(normalized), count);
            if (count != null && count >= MAX_FAILURES) {
                generalRedisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_TTL_MINUTES, TimeUnit.MINUTES);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw redisUnavailable(e);
        }
        throw authFailed();
    }

    public void clearOnSuccess(String accountTable, String email) {
        String normalized = AccountIdentityService.normalizeEmail(email);
        try {
            generalRedisTemplate.delete(attemptsKey(accountTable, normalized));
            generalRedisTemplate.delete(lockKey(accountTable, normalized));
        } catch (Exception e) {
            throw redisUnavailable(e);
        }
    }

    public ApiException authFailed() {
        return new ApiException(ErrorType.INVALID_CREDENTIALS, ErrorType.INVALID_CREDENTIALS.getDefaultMessage());
    }

    public ApiException redisUnavailable(Exception cause) {
        log.warn("Auth Redis unavailable: {}", cause.toString());
        return new ApiException(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE);
    }

    private static String attemptsKey(String accountTable, String normalizedEmail) {
        return "auth:login:" + accountTable + ":attempts:" + normalizedEmail;
    }

    private static String lockKey(String accountTable, String normalizedEmail) {
        return "auth:login:" + accountTable + ":lock:" + normalizedEmail;
    }

    private static String hashEmail(String email) {
        if (email == null) {
            return "null";
        }
        return Integer.toHexString(email.hashCode());
    }
}
