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
import com.coursistant.lms.module.auth.session.dto.AuthResult;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.chat.entity.Query;
import com.coursistant.lms.module.chat.service.CoursistanceService;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.service.UserService;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import com.coursistant.lms.shared.idempotency.Idempotent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
import java.util.concurrent.TimeUnit;
import java.time.ZoneId;

/**
 * Auth session endpoints under /v1/auth.
 * Health check is at /v1; /query is kept at root until moved to chat module.
 */
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
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/v1")
    public ApiResponse<String> hello() {
        return ApiResponse.success("访问成功");
    }

    @PostMapping("/v1/auth/login")
    public ApiResponse<AuthResult> login(@RequestBody Account account, HttpServletResponse response) {
        if (ObjectUtil.isEmpty(account.getEmail()) || ObjectUtil.isEmpty(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        AuthResult result = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            result = adminService.login(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            result = userService.login(account);
        }

        if (result == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        setRefreshTokenCookie(response, result.getRefreshToken());
        return ApiResponse.success(result);
    }

    @PostMapping("/v1/auth/refresh-token")
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
        setRefreshTokenCookie(response, result.getRefreshToken());
        return ApiResponse.success(result.getAccessToken());
    }

    @PostMapping("/v1/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
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

        if (StrUtil.isNotBlank(refreshToken)) {
            refreshTokenService.deleteByToken(refreshToken);
        }

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).path("/")
                .maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.success();
    }

    @PostMapping("/v1/auth/register")
    public ApiResponse<AuthResult> register(@RequestBody Account account, HttpServletResponse response) {
        if (StrUtil.isBlank(account.getPassword()) || StrUtil.isBlank(account.getEmail())
                || StrUtil.isBlank(account.getName())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        AuthResult result = userService.register(account);
        setRefreshTokenCookie(response, result.getRefreshToken());
        return ApiResponse.success(result);
    }

    @PostMapping("/v1/auth/email-verifications/register")
    public ApiResponse<Void> sendRegisterEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "register");
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/v1/auth/email-verifications/register/validate")
    public ApiResponse<Void> validateRegisterEmailVerification(@RequestParam("email") String email,
                                                    @RequestParam("code") String code) {
        userService.validateEmailVerificationCode(email, code, "register");
        return ApiResponse.success();
    }

    @PostMapping("/v1/auth/email-verifications/reset")
    public ApiResponse<Void> sendResetEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "reset");
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/v1/auth/email-verifications/reset/validate")
    public ApiResponse<Void> validateResetEmailVerification(@RequestParam("email") String email,
                                                 @RequestParam("code") String code) {
        userService.validateEmailVerificationCode(email, code, "reset");
        return ApiResponse.success();
    }

    @Idempotent
    @PutMapping("/v1/auth/password")
    public ApiResponse<Void> updatePassword(@RequestBody PasswordDTO account) {
        if (!"update".equals(account.getType())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        if (StrUtil.isBlank(account.getEmail()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getNewPassword())) {
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

    @Idempotent
    @PostMapping("/v1/auth/password-resets")
    public ApiResponse<Void> resetPassword(@RequestBody PasswordDTO dto) {
        if (StrUtil.isBlank(dto.getEmail()) || StrUtil.isBlank(dto.getNewPassword())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        userService.resetPassword(dto.getEmail(), dto.getNewPassword());
        return ApiResponse.success();
    }

    /**
     * Chat query — kept at /query until moved to chat module.
     */
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

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24 * 14)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
