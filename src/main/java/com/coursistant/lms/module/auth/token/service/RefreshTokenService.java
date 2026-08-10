package com.coursistant.lms.module.auth.token.service;

import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final String REDIS_PREFIX = "refresh:token:";
    private static final String REDIS_SESSION_PREFIX = "refresh:session:";
    private static final int MAX_DEVICES = 5;

    @Resource
    private RefreshTokenMapper refreshTokenMapper;

    @Resource(name = "refreshTokenRedisTemplate")
    private RedisTemplate<String, Object> refreshTokenRedisTemplate;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private Clock clock;

    @Value("${token.refresh-expire-days:14}")
    private int refreshExpireDays;

    @Value("${token.refresh-rotation-grace-seconds:30}")
    private int refreshRotationGraceSeconds;

    public String createAndStoreRefreshToken(Integer userId, String role) {
        requireRedis();

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String token = UUID.randomUUID().toString().replace("-", "");

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        List<RefreshToken> existingTokens = refreshTokenMapper.selectByUserIdAndRoleOrderByCreateTime(userId, role);
        while (existingTokens.size() >= MAX_DEVICES) {
            RefreshToken oldest = existingTokens.remove(0);
            refreshTokenMapper.deleteById(oldest.getId());
            bestEffortDeleteRedis(oldest);
        }

        writeRedisSession(sessionId, token, userId, role);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSessionId(sessionId);
        refreshToken.setUserId(userId);
        refreshToken.setRole(role);
        refreshToken.setToken(token);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setExpireTime(Date.from(LocalDateTime.now(clock).plusDays(refreshExpireDays)
                .atZone(ZoneId.systemDefault()).toInstant()));
        refreshTokenMapper.insert(refreshToken);

        return token;
    }

    public boolean validateRefreshToken(String token) {
        requireRedis();
        String redisKey = REDIS_PREFIX + token;
        if (Boolean.TRUE.equals(refreshTokenRedisTemplate.hasKey(redisKey))) {
            return true;
        }

        RefreshToken dbToken = refreshTokenMapper.selectByToken(token);
        if (dbToken == null) {
            return false;
        }
        Date now = Date.from(Instant.now(clock));
        boolean isValid = dbToken.getExpireTime() != null && dbToken.getExpireTime().after(now)
                && token.equals(dbToken.getToken());
        if (!isValid && token.equals(dbToken.getToken())) {
            refreshTokenMapper.deleteById(dbToken.getId());
        }
        return isValid;
    }

    /**
     * Atomic rotation with configurable grace. Replay outside grace revokes only this device session.
     * Only {@link RefreshTokenReusedException} skips rollback so the revoke commits.
     */
    @Transactional(noRollbackFor = RefreshTokenReusedException.class)
    public RefreshResult getNewAccessToken(String token) {
        requireRedis();

        Object sessionIdObj = refreshTokenRedisTemplate.opsForValue().get(REDIS_PREFIX + token);
        String sessionId = null;
        if (sessionIdObj != null) {
            String payload = sessionIdObj.toString();
            // format sessionId:userId:role OR legacy userId:role
            String[] parts = payload.split(":", 3);
            if (parts.length >= 3) {
                sessionId = parts[0];
            }
        }

        RefreshToken located = refreshTokenMapper.selectByToken(token);
        if (located == null) {
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }
        if (sessionId == null) {
            sessionId = located.getSessionId();
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }

        RefreshToken locked = refreshTokenMapper.selectBySessionIdForUpdate(sessionId);
        if (locked == null) {
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }
        Date now = Date.from(Instant.now(clock));
        if (locked.getExpireTime() != null && locked.getExpireTime().before(now)) {
            refreshTokenMapper.deleteById(locked.getId());
            bestEffortDeleteRedis(locked);
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }

        if (token.equals(locked.getToken())) {
            String newRefreshToken = UUID.randomUUID().toString().replace("-", "");
            locked.setPreviousToken(locked.getToken());
            locked.setPreviousValidUntil(Date.from(now.toInstant().plusSeconds(refreshRotationGraceSeconds)));
            locked.setToken(newRefreshToken);
            locked.setExpireTime(Date.from(LocalDateTime.now(clock).plusDays(refreshExpireDays)
                    .atZone(ZoneId.systemDefault()).toInstant()));
            locked.setUpdatedTime(now);
            refreshTokenMapper.updateById(locked);

            writeRedisSession(locked.getSessionId(), newRefreshToken, locked.getUserId(), locked.getRole());
            bestEffortDeleteKey(REDIS_PREFIX + token);

            String accessToken = issueAccessToken(locked.getUserId(), locked.getRole());
            return new RefreshResult(accessToken, newRefreshToken);
        }

        if (token.equals(locked.getPreviousToken())) {
            if (locked.getPreviousValidUntil() != null && !now.after(locked.getPreviousValidUntil())) {
                String accessToken = issueAccessToken(locked.getUserId(), locked.getRole());
                return new RefreshResult(accessToken, locked.getToken());
            }
            log.warn("Refresh token replay outside grace for session {}", sessionId);
            refreshTokenMapper.deleteBySessionId(sessionId);
            bestEffortDeleteRedis(locked);
            throw new RefreshTokenReusedException();
        }

        throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
    }

    public void deleteByUserId(Integer userId, String role) {
        List<RefreshToken> tokens = refreshTokenMapper.selectAllByUserId(userId);
        refreshTokenMapper.deleteByUserId(userId);
        for (RefreshToken t : tokens) {
            bestEffortDeleteRedis(t);
        }
    }

    /**
     * Logout: DB revoke succeeds even if Redis cleanup fails.
     */
    public void deleteByToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        RefreshToken existing = null;
        try {
            existing = refreshTokenMapper.selectByToken(token);
        } catch (Exception ignored) {
            // fall through to best-effort token delete
        }
        refreshTokenMapper.deleteByToken(token);
        if (existing != null) {
            bestEffortDeleteRedis(existing);
        } else {
            bestEffortDeleteKey(REDIS_PREFIX + token);
        }
    }

    public RefreshToken getByToken(String token) {
        return refreshTokenMapper.selectByToken(token);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTokens() {
        refreshTokenMapper.deleteExpiredTokens();
    }

    private void writeRedisSession(String sessionId, String token, Integer userId, String role) {
        requireRedis();
        String payload = sessionId + ":" + userId + ":" + role;
        refreshTokenRedisTemplate.opsForValue().set(
                REDIS_PREFIX + token, payload, Duration.ofDays(refreshExpireDays));
        refreshTokenRedisTemplate.opsForValue().set(
                REDIS_SESSION_PREFIX + sessionId, token, Duration.ofDays(refreshExpireDays));
    }

    private String issueAccessToken(Integer userId, String role) {
        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            Admin admin = adminMapper.selectById(userId);
            int authVersion = admin == null || admin.getAuthVersion() == null ? 1 : admin.getAuthVersion();
            return TokenUtils.createAccessToken(userId, role, authVersion, null);
        }
        User user = userMapper.selectById(userId);
        int authVersion = user == null || user.getAuthVersion() == null ? 1 : user.getAuthVersion();
        Integer tenantSecurityVersion = 1;
        if (user != null && user.getTenantId() != null) {
            Tenant tenant = tenantMapper.selectById(user.getTenantId());
            if (tenant != null && tenant.getSecurityVersion() != null) {
                tenantSecurityVersion = tenant.getSecurityVersion();
            }
        }
        return TokenUtils.createAccessToken(userId, role, authVersion, tenantSecurityVersion);
    }

    private void requireRedis() {
        try {
            Boolean pong = refreshTokenRedisTemplate.hasKey("__auth_refresh_ping__");
            // hasKey against missing key is fine; connection failure throws
            if (pong != null && pong) {
                // no-op
            }
        } catch (Exception e) {
            log.warn("Refresh Redis unavailable: {}", e.toString());
            throw new ApiException(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE);
        }
    }

    private void bestEffortDeleteRedis(RefreshToken token) {
        if (token == null) {
            return;
        }
        if (token.getToken() != null) {
            bestEffortDeleteKey(REDIS_PREFIX + token.getToken());
        }
        if (token.getPreviousToken() != null) {
            bestEffortDeleteKey(REDIS_PREFIX + token.getPreviousToken());
        }
        if (token.getSessionId() != null) {
            bestEffortDeleteKey(REDIS_SESSION_PREFIX + token.getSessionId());
        }
    }

    private void bestEffortDeleteKey(String key) {
        try {
            refreshTokenRedisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Best-effort Redis delete failed for key prefix {}", key.substring(0, Math.min(16, key.length())));
        }
    }
}
