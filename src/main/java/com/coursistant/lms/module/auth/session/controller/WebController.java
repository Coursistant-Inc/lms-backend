package com.coursistant.lms.module.auth.session.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.user.entity.Account;
import com.coursistant.lms.module.auth.admin.dto.PasswordDTO;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.chat.entity.Query;
import com.coursistant.lms.module.chat.service.CoursistanceService;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.service.UserService;
import com.coursistant.lms.shared.security.TokenUtils;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import com.coursistant.lms.shared.security.RequiresPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.coursistant.lms.module.groupchat.service.RocketChatAuthService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.ZoneId;

@Slf4j
@RestController
public class WebController {

    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private CoursistanceService coursistanceService;
    @Resource
    private RefreshTokenService refreshTokenService;
    @Resource
    private RocketChatAuthService rocketChatAuthService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @GetMapping("/")
    public ApiResponse<String> hello() {
        return ApiResponse.success("访问成功");
    }

    @PostMapping("/login")
    public ApiResponse<Account> login(@RequestBody Account account, HttpServletResponse response) {
        if (ObjectUtil.isEmpty(account.getEmail()) || ObjectUtil.isEmpty(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        Account dbAccount = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            dbAccount = adminService.login(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            dbAccount = userService.login(account);
        }

        if (dbAccount == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        if (ObjectUtil.isNotEmpty(dbAccount.getRefreshToken())) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", dbAccount.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(60 * 60 * 24 * 30)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        dbAccount.setRefreshToken(null);

        try {
            rocketChatAuthService.ensureUserExists(
                    dbAccount.getEmail(),
                    account.getPassword(),
                    dbAccount.getName()
            );

            Map<String, String> rcToken = rocketChatAuthService.createTokenForUser(dbAccount.getEmail());

            dbAccount.setRocketChatToken(rcToken.get("authToken"));
            dbAccount.setRocketChatUserId(rcToken.get("userId"));

            log.info("RocketChat token created for: {}", dbAccount.getEmail());

        } catch (Exception e) {
            log.warn("RocketChat login failed: {}", e.getMessage());
        }

        return ApiResponse.success(dbAccount);
    }

    @PostMapping("/refresh-token")
    public ApiResponse<String> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String rateLimitKey = "ratelimit:refresh:" + request.getRemoteAddr();
        Long count = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(rateLimitKey, 60, TimeUnit.SECONDS);
        }
        if (count != null && count > 10) {
            throw new ApiException(ErrorType.TOO_MANY_REQUESTS, "Too many requests, please try again later");
        }

        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (StrUtil.isBlank(refreshToken)) {
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }

        RefreshResult result = refreshTokenService.getNewAccessToken(refreshToken);

        ResponseCookie newCookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24 * 30)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, newCookie.toString());

        return ApiResponse.success(result.getAccessToken());
    }


    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");

        refreshTokenService.deleteByUserId(userId, role);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).path("/")
                .maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.success();
    }

    /**
     * 注册 // Register
     */
    @PostMapping("/register")
    public ApiResponse<Account> register(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getPassword()) || StrUtil.isBlank(account.getEmail())
                || ObjectUtil.isEmpty(account.getRole())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.register(account);
            account.setAccessToken(TokenUtils.createAccessToken(account.getId(), RoleEnum.ADMIN.name()));
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.register(account);
            account.setAccessToken(TokenUtils.createAccessToken(account.getId(), RoleEnum.USER.name()));
        }

        return ApiResponse.success(account);
    }

    /**
     * 发送邮箱验证码 // Send email verification code register
     */
    @PostMapping("/sendRegisterEmailVerification")
    public ApiResponse<Void> sendRegisterEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "register");
        return ApiResponse.success();
    }

    /**
     * 校验邮箱验证码 // Validate email verification code
     */
    @PostMapping("/validateRegisterEmailVerification")
    public ApiResponse<Void> validateRegisterEmailVerification(@RequestParam("email") String email,
                                                    @RequestParam("code") String code) {
        userService.validateEmailVerificationCode(email, code);
        return ApiResponse.success();
    }


    @PostMapping("/sendResetEmailVerification")
    public ApiResponse<Void> sendResetEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "reset");
        return ApiResponse.success();
    }


    /**
     * 修改密码 // Update password
     */
    @PutMapping("/updatePassword")
    public ApiResponse<Void> updatePassword(@RequestBody PasswordDTO account) {
        if ("update".equals(account.getType())) {
            if (StrUtil.isBlank(account.getEmail()) || StrUtil.isBlank(account.getPassword())
                    || ObjectUtil.isEmpty(account.getNewPassword())) {
                throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
            }
        } else if ("reset".equals(account.getType())) {
            if (StrUtil.isBlank(account.getEmail()) || ObjectUtil.isEmpty(account.getNewPassword())) {
                throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
            }
        } else {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.updatePassword(account);
        }
        return ApiResponse.success();
    }

    @PostMapping("/resetPasswordValidation")
    public ApiResponse<String> resetPasswordValidation(@RequestBody PasswordDTO account) {

        if (StrUtil.isBlank(account.getEmail()) || StrUtil.isBlank(account.getCode())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        String token = userService.resetPasswordValidation(account);

        return ApiResponse.success(token);
    }

    /**
     * 处理查询请求 // Handle query request
     */
    @RequiresPermission("chatbot:interact")
    @PostMapping(value = "/query", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Query> query(@RequestParam(value = "file", required = false) MultipartFile file,
                        @RequestParam("courseId") Integer courseId,
                        @RequestParam("query") String query,
                        @RequestParam("dialogueId") Integer dialogueId,
                        @RequestParam("userId") Integer userId,
                        @RequestHeader(value = "X-Timezone", required = false) String timezone) {

        String savedFilePath = "N/A";

        try {
            if (file != null && !file.isEmpty()) {
                String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
                String baseDir = "/home/ubuntu/SpringBoot/saved_images/query_images";
                String uploadDir = baseDir + datePath + "/";
                File dir = new File(uploadDir);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + uploadDir);
                }

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File destFile = new File(uploadDir + fileName);

                file.transferTo(destFile);
                savedFilePath = destFile.getAbsolutePath();

                log.info("File saved at: {}", savedFilePath);
            }
        } catch (Exception e) {
            log.error("Error saving file: {}", e.getMessage());
        }

        log.info("Start {}: {}", "query", String.format("filePath=%s, courseId=%s, query=%s, dialogueId=%d",
                savedFilePath, courseId, query, dialogueId));
        Query re_query;
        System.out.println(query);
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        if (file != null && !file.isEmpty()) {
            re_query = coursistanceService.query(new File(savedFilePath), courseId, query, dialogueId, userId, zone);
        } else {
            re_query = coursistanceService.query(null, courseId, query, dialogueId, userId, zone);
        }
        log.info("End {}: {}", "query", re_query.toString());
        return ApiResponse.success(re_query);
    }


}
