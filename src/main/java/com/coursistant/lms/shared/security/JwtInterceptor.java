package com.coursistant.lms.shared.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.user.entity.Account;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.service.UserService;
import com.coursistant.lms.shared.security.JwtParserUtil;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private JwtParserUtil jwtParserUtil;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String path = request.getRequestURI();
        if (path.contains("/rocketchat/")) {
            log.debug("RocketChat path, skipping JWT check: {}", path);
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        String token = authHeader.substring(7);

        DecodedJWT jwt = jwtParserUtil.verify(token);
        Integer userId = jwt.getClaim("userId").asInt();
        String role = jwt.getClaim("role").asString();

        if (userId == null || role == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }

        String cacheKey = "user:active:" + userId;
        Boolean exists = stringRedisTemplate.hasKey(cacheKey);
        if (Boolean.FALSE.equals(exists)) {
            Account account = null;
            if (RoleEnum.ADMIN.name().equals(role)) {
                account = adminService.selectById(userId);
            } else if (RoleEnum.USER.name().equals(role)) {
                account = userService.selectById(userId);
            }
            if (account == null) {
                throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
            }
            stringRedisTemplate.opsForValue().set(cacheKey, "1", 5, TimeUnit.MINUTES);
        }

        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);

        log.debug("Authentication succeed, userId: {}, role: {}", userId, role);
        return true;
    }
}
