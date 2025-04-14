package com.coursistant.individual.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.coursistant.individual.common.Constants;
import com.coursistant.individual.common.enums.ResultCodeEnum;
import com.coursistant.individual.common.enums.RoleEnum;
import com.coursistant.individual.entity.Account;
import com.coursistant.individual.exception.CustomException;
import com.coursistant.individual.service.system.AdminService;
import com.coursistant.individual.service.user.UserService;
import com.coursistant.individual.utils.RsaKeyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.interfaces.RSAPublicKey;

/**
 * jwt拦截器
 * JWT interceptor for authentication
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
            // TODO Auto-generated catch block
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return true;
    }
}
