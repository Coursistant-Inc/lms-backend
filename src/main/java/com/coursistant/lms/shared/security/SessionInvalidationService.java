package com.coursistant.lms.shared.security;

import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.enums.RoleEnum;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionInvalidationService {

    @Resource
    private RefreshTokenService refreshTokenService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void invalidatePrincipal(Integer principalId, String role) {
        if (principalId == null || role == null) {
            return;
        }
        refreshTokenService.deleteByUserId(principalId, role);
        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            stringRedisTemplate.delete(JwtInterceptor.PRINCIPAL_CACHE_ADMIN_PREFIX + principalId);
        } else {
            stringRedisTemplate.delete(JwtInterceptor.PRINCIPAL_CACHE_USER_PREFIX + principalId);
        }
    }
}
