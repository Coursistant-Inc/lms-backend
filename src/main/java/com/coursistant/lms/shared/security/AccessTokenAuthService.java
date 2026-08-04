package com.coursistant.lms.shared.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.RoleEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Shared access-token claim + principal snapshot validation for Filter and Interceptor.
 */
@Service
public class AccessTokenAuthService {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenAuthService.class);

    public record AuthenticatedPrincipal(Integer userId, String role) {}

    @Resource
    private JwtParserUtil jwtParserUtil;
    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${auth.jwt.min-issued-at:}")
    private String minIssuedAtConfig;

    public AuthenticatedPrincipal authenticateBearer(String authorizationHeader, HttpServletRequest request) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
        String token = authorizationHeader.substring(7);
        if (token.isBlank()) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
        DecodedJWT jwt = jwtParserUtil.verify(token);

        String type = jwt.getClaim("type").asString();
        if (type == null || !"access".equals(type)) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        Integer userId = jwt.getClaim("userId").asInt();
        String role = jwt.getClaim("role").asString();
        String sub = jwt.getSubject();
        if (userId == null || role == null || sub == null || sub.isBlank()) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
        if (!sub.equals(String.valueOf(userId))) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
        if ("ADMIN".equals(role)
                || !(RoleEnum.SYSTEM_ADMIN.name().equals(role)
                || RoleEnum.USER.name().equals(role)
                || RoleEnum.TENANT_ADMIN.name().equals(role))) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        rejectIfBeforeCutover(jwt.getIssuedAt());

        Integer tokenAuthVersion = jwt.getClaim("authVersion").asInt();
        Integer tokenTenantSecurityVersion = jwt.getClaim("tenantSecurityVersion").isNull()
                ? null
                : jwt.getClaim("tenantSecurityVersion").asInt();

        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            Admin admin = null;
        try {
            admin = loadAdmin(userId);
        } catch (ApiException e) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
        if (admin == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
            if (admin.getStatus() != null && !AccountStatus.ACTIVE.name().equals(admin.getStatus())) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
            int dbVersion = admin.getAuthVersion() == null ? 1 : admin.getAuthVersion();
            if (tokenAuthVersion == null || tokenAuthVersion != dbVersion) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
            if (!RoleEnum.SYSTEM_ADMIN.name().equals(admin.getRole())
                    && admin.getRole() != null
                    && !admin.getRole().equals(role)) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
        } else {
            User user = null;
        try {
            user = loadUser(userId);
        } catch (ApiException e) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
        if (user == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
            if (user.getStatus() != null && !AccountStatus.ACTIVE.name().equals(user.getStatus())) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
            if (user.getRole() != null && !user.getRole().equals(role)) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
            int dbVersion = user.getAuthVersion() == null ? 1 : user.getAuthVersion();
            if (tokenAuthVersion == null || tokenAuthVersion != dbVersion) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
            Tenant tenant = user.getTenantId() == null ? null : tenantMapper.selectById(user.getTenantId());
            if (tenant == null
                    || (tenant.getStatus() != null && !AccountStatus.ACTIVE.name().equals(tenant.getStatus()))) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
            int dbTenantSec = tenant.getSecurityVersion() == null ? 1 : tenant.getSecurityVersion();
            if (tokenTenantSecurityVersion == null || tokenTenantSecurityVersion != dbTenantSec) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
            }
        }

        request.setAttribute(AuthzService.ATTR_USER_ID, userId);
        request.setAttribute(AuthzService.ATTR_USER_ROLE, role);
        return new AuthenticatedPrincipal(userId, role);
    }

    private Admin loadAdmin(Integer userId) {
        String cacheKey = JwtInterceptor.PRINCIPAL_CACHE_ADMIN_PREFIX + userId;
        try {
            Boolean exists = stringRedisTemplate.hasKey(cacheKey);
            if (Boolean.TRUE.equals(exists)) {
                return adminService.selectById(userId);
            }
        } catch (Exception e) {
            log.debug("Principal cache unavailable for admin {}, falling back to DB", userId);
        }
        Admin admin = adminService.selectById(userId);
        cachePrincipalBestEffort(cacheKey);
        return admin;
    }

    private User loadUser(Integer userId) {
        String cacheKey = JwtInterceptor.PRINCIPAL_CACHE_USER_PREFIX + userId;
        try {
            Boolean exists = stringRedisTemplate.hasKey(cacheKey);
            if (Boolean.TRUE.equals(exists)) {
                return userService.selectById(userId);
            }
        } catch (Exception e) {
            log.debug("Principal cache unavailable for user {}, falling back to DB", userId);
        }
        User user = userService.selectById(userId);
        cachePrincipalBestEffort(cacheKey);
        return user;
    }

    private void cachePrincipalBestEffort(String cacheKey) {
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, "1", 5, TimeUnit.MINUTES);
        } catch (Exception ignored) {
            // Redis outage must not fail authenticated business requests.
        }
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
}
