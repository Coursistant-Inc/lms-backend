package com.coursistant.lms.module.auth.session.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.enums.LoginAccountType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.user.account.dto.RegisterRequest;
import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.auth.session.dto.AuthResult;
import com.coursistant.lms.module.auth.session.dto.ChangePasswordRequest;
import com.coursistant.lms.module.auth.session.dto.PasswordResetRequest;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.concurrent.TimeUnit;

/**
 * Auth session endpoints under /v1/auth.
 * Health check is at /v1.
 */
@Slf4j
@RestController
public class WebController {

    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private RefreshTokenService refreshTokenService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AuthzService authzService;

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

        LoginAccountType accountType = LoginAccountType.fromRequestRole(account.getRole());
        if (accountType == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        AuthResult result;
        if (accountType.routesToAdminTable()) {
            result = adminService.login(account);
        } else if (accountType.routesToUserTable()) {
            result = userService.login(account);
        } else {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        setRefreshTokenCookie(response, result.getRefreshToken());
        return ApiResponse.success(result);
    }

    @PostMapping("/v1/auth/refresh-token")
    public ApiResponse<String> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            String rateLimitKey = "ratelimit:refresh:" + request.getRemoteAddr();
            Long count = stringRedisTemplate.opsForValue().increment(rateLimitKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(rateLimitKey, 60, TimeUnit.SECONDS);
            }
            if (count != null && count > 10) {
                throw new ApiException(ErrorType.TOO_MANY_REQUESTS, "Too many requests, please try again later");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorType.AUTH_SERVICE_TEMPORARILY_UNAVAILABLE);
        }

        String refreshToken = readRefreshCookie(request);
        if (StrUtil.isBlank(refreshToken)) {
            clearRefreshTokenCookie(response);
            throw new ApiException(ErrorType.REFRESH_TOKEN_INVALID, "Refresh Token Validation Failed");
        }

        try {
            RefreshResult result = refreshTokenService.getNewAccessToken(refreshToken);
            setRefreshTokenCookie(response, result.getRefreshToken());
            return ApiResponse.success(result.getAccessToken());
        } catch (ApiException e) {
            if (e.getErrorType() == ErrorType.REFRESH_TOKEN_REUSED
                    || e.getErrorType() == ErrorType.REFRESH_TOKEN_INVALID) {
                clearRefreshTokenCookie(response);
            }
            throw e;
        }
    }

    @PostMapping("/v1/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request);
        if (StrUtil.isNotBlank(refreshToken)) {
            refreshTokenService.deleteByToken(refreshToken);
        }
        clearRefreshTokenCookie(response);
        return ApiResponse.success();
    }

    @PostMapping("/v1/auth/register")
    public ApiResponse<AuthResult> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        if (request == null || StrUtil.isBlank(request.getPassword()) || StrUtil.isBlank(request.getEmail())
                || StrUtil.isBlank(request.getName()) || StrUtil.isBlank(request.getVerificationCode())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        AuthResult result = userService.register(request);
        setRefreshTokenCookie(response, result.getRefreshToken());
        return ApiResponse.success(result);
    }

    @PostMapping("/v1/auth/email-verifications/register")
    public ApiResponse<Void> sendRegisterEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "register");
        return ApiResponse.success();
    }

    @PostMapping("/v1/auth/email-verifications/reset")
    public ApiResponse<Void> sendResetEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "reset");
        return ApiResponse.success();
    }

    @Idempotent
    @PutMapping("/v1/auth/password")
    public ApiResponse<Void> updatePassword(HttpServletRequest request, @RequestBody ChangePasswordRequest body) {
        if (body == null || StrUtil.isBlank(body.getCurrentPassword()) || StrUtil.isBlank(body.getNewPassword())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        Integer userId = authzService.requireUserId(request);
        String role = authzService.requireRole(request);
        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            adminService.updatePasswordForPrincipal(userId, body);
        } else if (RoleEnum.USER.name().equals(role) || RoleEnum.TENANT_ADMIN.name().equals(role)) {
            userService.updatePasswordForPrincipal(userId, body);
        } else {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/v1/auth/password-resets")
    public ApiResponse<Void> resetPassword(@RequestBody PasswordResetRequest dto) {
        if (dto == null || StrUtil.isBlank(dto.getEmail()) || StrUtil.isBlank(dto.getNewPassword())
                || StrUtil.isBlank(dto.getVerificationCode())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        userService.resetPassword(dto);
        return ApiResponse.success();
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
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

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).path("/")
                .maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
