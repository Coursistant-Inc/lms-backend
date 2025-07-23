package com.coursistant.lms.service.system;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.RefreshToken;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.system.RefreshTokenMapper;
import com.coursistant.lms.utils.TokenUtils;
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

/**
 * Refresh Token 服务（无接口版）
 */
@Service
public class RefreshTokenService {

    private static final String REDIS_PREFIX = "refresh:";

    @Resource
    private RefreshTokenMapper refreshTokenMapper;

    @Resource(name = "refreshTokenRedisTemplate")
    private RedisTemplate<String, Object> refreshTokenRedisTemplate;

    @Value("${token.refresh-expire-days:30}")
    private int refreshExpireDays;

    /**
     * 创建并保存 refresh token（写入 Redis + MySQL）
     */
    public String createAndStoreRefreshToken(Integer userId, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        // 限制每个用户角色最多只能有 3 条 refresh token，超过删除最旧的一条
        List<RefreshToken> existingTokens = refreshTokenMapper.selectByUserIdAndRoleOrderByCreateTime(userId, role);
        if (existingTokens.size() >= 3) {
            RefreshToken oldest = existingTokens.get(0); // 假设按 create_time 升序排列
            refreshTokenMapper.deleteById(oldest.getId());
        }

        // 存入 Redis
        String redisKey = REDIS_PREFIX + userId+ role;
        refreshTokenRedisTemplate.opsForValue().set(redisKey, token, Duration.ofDays(refreshExpireDays));

        // 存入数据库
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

    /**
     * 校验 refresh token 是否有效
     */
    public boolean validateRefreshToken(String token) {
        RefreshToken dbToken = refreshTokenMapper.selectByToken(token);
        if (dbToken == null) return false;

        String redisKey = REDIS_PREFIX + dbToken.getUserId();
        Object redisTokenObj = refreshTokenRedisTemplate.opsForValue().get(redisKey);
        String redisToken = redisTokenObj != null ? redisTokenObj.toString() : null;

        if (redisToken != null) {
            return redisToken.equals(token);
        }

        // Redis 失效，查数据库时间兜底 + 自动清理过期记录
        boolean isValid = dbToken.getExpireTime().after(new Date());
        if (!isValid) {
            refreshTokenMapper.deleteById(dbToken.getId());
        }
        return isValid;
    }

    //get new access token
    public String getNewAccessToken(String token){

        if (!validateRefreshToken(token)){
            throw new CustomException(ResultCodeEnum.REFRESH_TOKEN_CHECK_ERROR);
        }
        RefreshToken dbToken = refreshTokenMapper.selectByToken(token);
        String tokenData = dbToken.getId() + "-" + dbToken.getRole();
        String accessToken = TokenUtils.createAccessToken(tokenData);

        Date newExpireTime = Date.from(LocalDateTime.now()
                .plusDays(refreshExpireDays)
                .atZone(ZoneId.systemDefault())
                .toInstant());

        // 更新 Redis
        String redisKey = REDIS_PREFIX + dbToken.getUserId() + dbToken.getRole();
        refreshTokenRedisTemplate.opsForValue().set(redisKey, token, Duration.ofDays(refreshExpireDays));

        // 更新数据库
        dbToken.setExpireTime(newExpireTime);
        dbToken.setUpdatedTime(new Date());
        refreshTokenMapper.updateById(dbToken);

        return accessToken;

    }

    /**
     * 删除指定用户的 refresh token（登出或强制下线）
     */
    public void deleteByUserId(Integer userId) {
        refreshTokenMapper.deleteByUserId(userId);
        refreshTokenRedisTemplate.delete(REDIS_PREFIX + userId);
    }

    /**
     * 根据 token 查询完整实体
     */
    public RefreshToken getByToken(String token) {
        return refreshTokenMapper.selectByToken(token);
    }

    /**
     * 每天定时清理过期 refresh token
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点
    public void cleanExpiredTokens() {
        refreshTokenMapper.deleteExpiredTokens();
    }
}
