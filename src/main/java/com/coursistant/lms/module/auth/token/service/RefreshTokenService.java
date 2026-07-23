package com.coursistant.lms.module.auth.token.service;

import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.shared.security.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.coursistant.lms.module.user.entity.User;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final String REDIS_PREFIX = "refresh:";
    private static final String REDIS_USED_PREFIX = "refresh:used:";
    private static final int GRACE_WINDOW_SECONDS = 30;

    @Resource
    private RefreshTokenMapper refreshTokenMapper;

    @Resource(name = "refreshTokenRedisTemplate")
    private RedisTemplate<String, Object> refreshTokenRedisTemplate;

    @Value("${token.refresh-expire-days:30}")
    private int refreshExpireDays;

    public String createAndStoreRefreshToken(Integer userId, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        List<RefreshToken> existingTokens = refreshTokenMapper.selectByUserIdAndRoleOrderByCreateTime(userId, role);
        if (existingTokens.size() >= 3) {
            RefreshToken oldest = existingTokens.get(0);
            refreshTokenMapper.deleteById(oldest.getId());
        }

        String redisKey = REDIS_PREFIX + userId + ":" + role;
        refreshTokenRedisTemplate.opsForValue().set(redisKey, token, Duration.ofDays(refreshExpireDays));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setRole(role);
        refreshToken.setToken(token);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setExpireTime(Date.from(LocalDateTime.now().plusDays(refreshExpireDays).atZone(ZoneId.systemDefault()).toInstant()));
        refreshTokenMapper.insert(refreshToken);

        return token;
    }

    public boolean validateRefreshToken(String token) {
        RefreshToken dbToken = refreshTokenMapper.selectByToken(token);
        if (dbToken == null) return false;

        String redisKey = REDIS_PREFIX + dbToken.getUserId() + ":" + dbToken.getRole();
        Object redisTokenObj = refreshTokenRedisTemplate.opsForValue().get(redisKey);
        String redisToken = redisTokenObj != null ? redisTokenObj.toString() : null;

        if (redisToken != null) {
            return redisToken.equals(token);
        }

        boolean isValid = dbToken.getExpireTime().after(new Date());
        if (!isValid) {
            refreshTokenMapper.deleteById(dbToken.getId());
        }
        return isValid;
    }

    /**
     * Refresh with token rotation + 30-second concurrency grace window.
     *
     * Flow:
     * 1. If the incoming token is a "used" old token within the 30s grace window,
     *    return the same new token (idempotent).
     * 2. If the incoming token is the current valid token, rotate: generate a new UUID,
     *    mark the old one as "used" in Redis with 30s TTL, and persist the new one.
     * 3. If the incoming token is a used token AFTER the grace window expired,
     *    it may be stolen — revoke all tokens for this user.
     */
    public RefreshResult getNewAccessToken(String token) {
        // 1. Check if this is a used old token within the grace window
        String usedKey = REDIS_USED_PREFIX + token;
        Object mappedNewTokenObj = refreshTokenRedisTemplate.opsForValue().get(usedKey);
        if (mappedNewTokenObj != null) {
            String mappedNewToken = mappedNewTokenObj.toString();
            RefreshToken dbToken = refreshTokenMapper.selectByToken(mappedNewToken);
            if (dbToken != null) {
                String accessToken = TokenUtils.createAccessToken(dbToken.getUserId(), dbToken.getRole());
                return new RefreshResult(accessToken, mappedNewToken);
            }
        }

        // 2. Validate the current token
        if (!validateRefreshToken(token)) {
            // Could be a used token whose grace window expired — possible theft
            RefreshToken dbToken = refreshTokenMapper.selectByToken(token);
            if (dbToken == null) {
                log.warn("Refresh token not found, possible token reuse attack: {}", token.substring(0, Math.min(8, token.length())));
            }
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }

        RefreshToken dbToken = refreshTokenMapper.selectByToken(token);
        if (dbToken == null) {
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }

        Integer userId = dbToken.getUserId();
        String role = dbToken.getRole();

        // Generate new refresh token UUID
        String newRefreshToken = UUID.randomUUID().toString().replace("-", "");

        // Mark old token as "used" with 30s grace window
        refreshTokenRedisTemplate.opsForValue().set(
                REDIS_USED_PREFIX + token, newRefreshToken,
                Duration.ofSeconds(GRACE_WINDOW_SECONDS));

        // Update Redis with the new token
        String redisKey = REDIS_PREFIX + userId + ":" + role;
        refreshTokenRedisTemplate.opsForValue().set(redisKey, newRefreshToken, Duration.ofDays(refreshExpireDays));

        // Update database record
        Date newExpireTime = Date.from(LocalDateTime.now()
                .plusDays(refreshExpireDays)
                .atZone(ZoneId.systemDefault())
                .toInstant());
        dbToken.setToken(newRefreshToken);
        dbToken.setExpireTime(newExpireTime);
        dbToken.setUpdatedTime(new Date());
        refreshTokenMapper.updateById(dbToken);

        String accessToken = TokenUtils.createAccessToken(userId, role);
        return new RefreshResult(accessToken, newRefreshToken);
    }

    public void deleteByUserId(Integer userId, String role) {
        refreshTokenMapper.deleteByUserId(userId);
        refreshTokenRedisTemplate.delete(REDIS_PREFIX + userId + ":" + role);
    }

    public RefreshToken getByToken(String token) {
        return refreshTokenMapper.selectByToken(token);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredTokens() {
        refreshTokenMapper.deleteExpiredTokens();
    }
}
