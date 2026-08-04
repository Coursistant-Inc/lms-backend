package com.coursistant.lms.module.auth.session.service;

import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Email verification send limits + atomic consume (no shared verified mark).
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    public static final String TYPE_REGISTER = "register";
    public static final String TYPE_RESET = "reset";

    public enum ConsumeResult {
        SUCCESS,
        INVALID,
        EXPIRED,
        ATTEMPTS_EXCEEDED
    }

    private static final String CONSUME_LUA =
            "local codeKey = KEYS[1]\n"
                    + "local attemptsKey = KEYS[2]\n"
                    + "local expected = ARGV[1]\n"
                    + "local maxAttempts = tonumber(ARGV[2])\n"
                    + "local attemptsTtl = tonumber(ARGV[3])\n"
                    + "local attempts = redis.call('GET', attemptsKey)\n"
                    + "if attempts and tonumber(attempts) >= maxAttempts then\n"
                    + "  return 'ATTEMPTS_EXCEEDED'\n"
                    + "end\n"
                    + "local cached = redis.call('GET', codeKey)\n"
                    + "if not cached then\n"
                    + "  return 'EXPIRED'\n"
                    + "end\n"
                    + "if cached ~= expected then\n"
                    + "  local next = redis.call('INCR', attemptsKey)\n"
                    + "  if next == 1 then\n"
                    + "    redis.call('EXPIRE', attemptsKey, attemptsTtl)\n"
                    + "  end\n"
                    + "  if next >= maxAttempts then\n"
                    + "    return 'ATTEMPTS_EXCEEDED'\n"
                    + "  end\n"
                    + "  return 'INVALID'\n"
                    + "end\n"
                    + "redis.call('DEL', codeKey)\n"
                    + "redis.call('DEL', attemptsKey)\n"
                    + "return 'SUCCESS'\n";

    private final DefaultRedisScript<String> consumeScript = new DefaultRedisScript<>(CONSUME_LUA, String.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LoginGuardService loginGuardService;

    public void assertCanSend(String type, String email) {
        String normalized = AccountIdentityService.normalizeEmail(email);
        requireType(type);
        String cooldownKey = "email:verification:cooldown:" + type + ":" + normalized;
        String hourlyKey = "email:verification:hourly:" + type + ":" + normalized;
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
                throw new ApiException(ErrorType.VERIFICATION_RESEND_COOLDOWN);
            }
            String hourlyRaw = stringRedisTemplate.opsForValue().get(hourlyKey);
            Integer hourlyCount = hourlyRaw == null ? null : Integer.parseInt(hourlyRaw);
            if (hourlyCount != null && hourlyCount >= 5) {
                throw new ApiException(ErrorType.VERIFICATION_HOURLY_LIMIT);
            }
            stringRedisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);
            if (hourlyCount == null) {
                stringRedisTemplate.opsForValue().set(hourlyKey, "1", 1, TimeUnit.HOURS);
            } else {
                Long next = stringRedisTemplate.opsForValue().increment(hourlyKey);
                if (next != null && next == 1L) {
                    stringRedisTemplate.expire(hourlyKey, 1, TimeUnit.HOURS);
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw loginGuardService.redisUnavailable(e);
        }
    }

    public void storeCode(String type, String email, String code) {
        String normalized = AccountIdentityService.normalizeEmail(email);
        requireType(type);
        String redisKey = "email:verification:" + type + ":" + normalized;
        try {
            stringRedisTemplate.opsForValue().set(redisKey, code, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw loginGuardService.redisUnavailable(e);
        }
    }

    /**
     * Atomically validate and consume a verification code. Never creates email:verified:*.
     */
    public ConsumeResult consume(String type, String email, String code) {
        String normalized = AccountIdentityService.normalizeEmail(email);
        requireType(type);
        if (code == null || code.isBlank()) {
            return ConsumeResult.INVALID;
        }
        String codeKey = "email:verification:" + type + ":" + normalized;
        String attemptsKey = "email:verification:attempts:" + type + ":" + normalized;
        try {
            List<String> keys = Arrays.asList(codeKey, attemptsKey);
            String raw = stringRedisTemplate.execute(consumeScript, keys, code, "5", String.valueOf(10 * 60));
            if (raw == null) {
                return ConsumeResult.EXPIRED;
            }
            return ConsumeResult.valueOf(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Unexpected consume result");
            return ConsumeResult.INVALID;
        } catch (Exception e) {
            throw loginGuardService.redisUnavailable(e);
        }
    }

    public void requireConsumeSuccess(String type, String email, String code) {
        ConsumeResult result = consume(type, email, code);
        switch (result) {
            case SUCCESS -> {
            }
            case EXPIRED -> throw new ApiException(ErrorType.VERIFICATION_CODE_EXPIRED);
            case ATTEMPTS_EXCEEDED -> throw new ApiException(ErrorType.VERIFICATION_ATTEMPTS_EXCEEDED);
            default -> throw new ApiException(ErrorType.INVALID_VERIFICATION_CODE);
        }
    }

    private static void requireType(String type) {
        if (!TYPE_REGISTER.equals(type) && !TYPE_RESET.equals(type)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Invalid request data");
        }
    }
}
