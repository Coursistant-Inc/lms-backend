package com.coursistant.lms.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.coursistant.lms.common.Constants;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.common.enums.RoleEnum;
import com.coursistant.lms.entity.Account;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.service.system.AdminService;
import com.coursistant.lms.service.user.UserService;
import com.coursistant.lms.utils.JwtParserUtil;
import com.coursistant.lms.utils.RsaKeyUtil;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.interfaces.RSAPublicKey;

/**
 * jwt拦截器
 * JWT interceptor for authentication
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private JwtParserUtil jwtParserUtil;
    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        
        // ⭐ 白名单：RocketChat iframe-auth 不需要验证 token
        String path = request.getRequestURI();
        if (path.contains("/rocketchat/")) {
            System.out.println("✅ RocketChat path, skipping JWT check: " + path);
            return true;
        }

        String standardAuthHeader = request.getHeader("Authorization");
        String standardToken;
        Integer userIdFromStandardToken;

        if (standardAuthHeader != null && standardAuthHeader.startsWith("Bearer ")) {
            standardToken = standardAuthHeader.substring(7);
            try {
                userIdFromStandardToken = jwtParserUtil.getUserId(standardToken);

                request.setAttribute("userId", userIdFromStandardToken);
                request.setAttribute("userRole", jwtParserUtil.getRole(standardToken));
                request.setAttribute("authType", "standard");

                log.info("Authentication succeed using standard Authorization header, userId: {}", userIdFromStandardToken);
                return true;
            } catch (Exception e) {
                log.warn("Standard Authorization header failed to parse, try legacy token: {}", e.getMessage());
            }
        }
        
        // 1. 从http请求的header中获取token
        // Retrieve the token from the HTTP request header
        String token = request.getHeader(Constants.TOKEN);
        if (ObjectUtil.isEmpty(token)) {
            // 如果没拿到，从参数里再拿一次
            // If not found in the header, try to get it from request parameters
            token = request.getParameter(Constants.TOKEN);
        }
        // 2. 开始执行认证
        // Start authentication process
        if (ObjectUtil.isEmpty(token)) {
            System.out.println(request.getContextPath());
            System.out.println(request.getRequestURI());
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        Account account = null;
        try {
            // 解析token获取存储的数据
            // Decode the token to extract stored data
            String userRole = JWT.decode(token).getAudience().get(0);
            String userId = userRole.split("-")[0];
            String role = userRole.split("-")[1];
            // 根据userId查询数据库
            // Query the database based on userId
            if (RoleEnum.ADMIN.name().equals(role)) {
                account = adminService.selectById(Integer.valueOf(userId));
            }
            if (RoleEnum.USER.name().equals(role)) {
                account = userService.selectById(Integer.valueOf(userId));
            }
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        if (ObjectUtil.isNull(account)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        try {
            // 用户密码加签验证 token
            // Verify the token using the user's password signature
            RSAPublicKey publicKey = (RSAPublicKey) RsaKeyUtil.loadPublicKey("public.pem");
            JWTVerifier jwtVerifier = JWT.require(Algorithm.RSA256(publicKey, null)).build();
            jwtVerifier.verify(token); // 验证token / Validate the token
        } catch (JWTVerificationException e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return true;
    }
}
