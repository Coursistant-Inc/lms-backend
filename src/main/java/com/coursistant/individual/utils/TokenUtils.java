package com.coursistant.individual.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coursistant.individual.common.Constants;
import com.coursistant.individual.common.enums.ResultCodeEnum;
import com.coursistant.individual.common.enums.RoleEnum;
import com.coursistant.individual.entity.Account;
import com.coursistant.individual.exception.CustomException;
import com.coursistant.individual.service.system.AdminService;
import com.coursistant.individual.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.security.interfaces.RSAPrivateKey;

/**
 * Token工具类 // Token Utility Class
 */
@Component
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    private static AdminService staticAdminService;
    private static UserService staticUserService;

    @Resource
    AdminService adminService;
    @Resource
    UserService userService;

    @PostConstruct
    public void setUserService() {
        staticAdminService = adminService;
        staticUserService = userService;
    }

    /**
     * 生成token // Generate token
     */
    public static String createAccessToken(String data) {
        RSAPrivateKey privateKey = null;
        try {
            privateKey = (RSAPrivateKey) RsaKeyUtil.loadPrivateKey("private.pem");
        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CREATION_ERROR);
        }
        return JWT.create().withAudience(data) // 将 userId-role 保存到 token 里面,作为载荷 // Store userId-role in the token as payload
                .withExpiresAt(DateUtil.offsetHour(new Date(), 2)) // 2小时后token过期 // Token expires after 2 hours
                .sign(Algorithm.RSA256(null, privateKey)); // 以 password 作为 token 的密钥 // Use password as the token's secret key
    }

    /**
     * 获取当前登录的用户信息 // Get current logged-in user information
     */
    public static Account getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader(Constants.TOKEN);
            if (ObjectUtil.isNotEmpty(token)) {
                String userRole = JWT.decode(token).getAudience().get(0);
                String userId = userRole.split("-")[0];  // 获取用户id // Get user ID
                String role = userRole.split("-")[1];    // 获取角色 // Get role
                if (RoleEnum.ADMIN.name().equals(role)) {
                    return staticAdminService.selectById(Integer.valueOf(userId));
                }
                if (RoleEnum.USER.name().equals(role)) {
                    return staticUserService.selectById(Integer.valueOf(userId));
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户信息出错", e); // Error retrieving current user information
        }
        return new Account();  // 返回空的账号对象 // Return an empty account object
    }
}
