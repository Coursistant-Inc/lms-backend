package com.coursistant.lms.shared.security;

import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.account.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.security.interfaces.RSAPrivateKey;

@Component
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    private static AdminService staticAdminService;
    private static UserService staticUserService;
    private static int staticAccessExpireHours = 2;
    private static RSAPrivateKey staticPrivateKey;
    private static String staticIssuer = "https://usc.xlearnedu.com";
    private static String staticAudience = "com.coursistant.lms";

    @Resource
    AdminService adminService;
    @Resource
    UserService userService;

    @Value("${token.access-expire-hours:2}")
    private int accessExpireHours;

    @Value("${token.private-key-path:private.pem}")
    private String privateKeyPath;

    @Value("${auth.jwt.issuer:https://usc.xlearnedu.com}")
    private String issuer;

    @Value("${auth.jwt.audience:com.coursistant.lms}")
    private String audience;

    @PostConstruct
    public void init() {
        staticAdminService = adminService;
        staticUserService = userService;
        staticAccessExpireHours = accessExpireHours;
        staticIssuer = issuer;
        staticAudience = audience;
        try {
            staticPrivateKey = (RSAPrivateKey) RsaKeyUtil.loadPrivateKey(privateKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key from: " + privateKeyPath, e);
        }
    }

    public static String createAccessToken(Integer userId, String role) {
        return createAccessToken(userId, role, 1, null);
    }

    public static String createAccessToken(Integer userId, String role, Integer authVersion, Integer tenantSecurityVersion) {
        var builder = JWT.create()
                .withSubject(userId.toString())
                .withClaim("userId", userId)
                .withClaim("role", role)
                .withClaim("type", "access")
                .withClaim("authVersion", authVersion == null ? 1 : authVersion)
                .withIssuedAt(new Date())
                .withExpiresAt(DateUtil.offsetHour(new Date(), staticAccessExpireHours))
                .withAudience(staticAudience)
                .withIssuer(staticIssuer);
        if (tenantSecurityVersion != null) {
            builder.withClaim("tenantSecurityVersion", tenantSecurityVersion);
        }
        return builder.sign(Algorithm.RSA256(null, staticPrivateKey));
    }

    /**
     * Read userId / userRole from request attributes set by JwtInterceptor,
     * then query the database for the full Account object.
     */
    public static Account getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            Integer userId = (Integer) request.getAttribute("userId");
            String role = (String) request.getAttribute("userRole");

            if (userId == null || role == null) {
                return new Account();
            }

            if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
                return staticAdminService.selectById(userId);
            }
            if (RoleEnum.USER.name().equals(role) || RoleEnum.TENANT_ADMIN.name().equals(role)) {
                return staticUserService.selectById(userId);
            }
        } catch (Exception e) {
            log.error("获取当前用户信息出错", e);
        }
        return new Account();
    }
}
