package com.coursistant.lms.shared.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String PRINCIPAL_CACHE_ADMIN_PREFIX = "auth:principal:admin:";
    public static final String PRINCIPAL_CACHE_USER_PREFIX = "auth:principal:user:";

    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private JwtParserUtil jwtParserUtil;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** ISO-8601 instant; tokens with iat before this are rejected. Empty disables cutover. */
    @Value("${auth.jwt.min-issued-at:}")
    private String minIssuedAtConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        String token = authHeader.substring(7);
        DecodedJWT jwt = jwtParserUtil.verify(token);

        String type = jwt.getClaim("type").asString();
        if (type != null && !"access".equals(type)) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        Integer userId = jwt.getClaim("userId").asInt();
        String role = jwt.getClaim("role").asString();
        if (userId == null || role == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        // Legacy ADMIN tokens are never accepted after RoleEnum cutover.
        if ("ADMIN".equals(role)) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        rejectIfBeforeCutover(jwt.getIssuedAt());

        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            ensurePrincipalExists(PRINCIPAL_CACHE_ADMIN_PREFIX + userId, () -> adminService.selectById(userId));
        } else if (RoleEnum.USER.name().equals(role) || RoleEnum.TENANT_ADMIN.name().equals(role)) {
            ensurePrincipalExists(PRINCIPAL_CACHE_USER_PREFIX + userId, () -> userService.selectById(userId));
        } else {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        request.setAttribute(AuthzService.ATTR_USER_ID, userId);
        request.setAttribute(AuthzService.ATTR_USER_ROLE, role);

        log.debug("Authentication succeed, userId: {}, role: {}", userId, role);
        return true;
    }

    private void rejectIfBeforeCutover(Date issuedAt) {
        if (minIssuedAtConfig == null || minIssuedAtConfig.isBlank()) {
            return;
        }
        Instant minIssuedAt = Instant.parse(minIssuedAtConfig.trim());
        if (issuedAt == null || issuedAt.toInstant().isBefore(minIssuedAt)) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
    }

    private void ensurePrincipalExists(String cacheKey, PrincipalLoader loader) {
        Boolean exists = stringRedisTemplate.hasKey(cacheKey);
        if (Boolean.FALSE.equals(exists) || exists == null) {
            Account account = loader.load();
            if (account == null) {
                throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
            }
            stringRedisTemplate.opsForValue().set(cacheKey, "1", 5, TimeUnit.MINUTES);
        }
    }

    @FunctionalInterface
    private interface PrincipalLoader {
        Account load();
    }
}
