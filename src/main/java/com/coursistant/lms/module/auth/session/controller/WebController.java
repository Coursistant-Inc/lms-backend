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
import com.coursistant.lms.module.auth.session.dto.LoginRequest;
import com.coursistant.lms.module.auth.session.dto.PasswordResetRequest;
import com.coursistant.lms.module.auth.token.dto.RefreshResult;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;

import java.util.concurrent.TimeUnit;

/**
 * Auth session endpoints under /v1/auth.
 * Health check is at /v1.
 */
@Slf4j
@RestController
@Tag(name = "Auth", description = "Session login, register, token refresh, password flows")
public class WebController {

    private static final String REFRESH_COOKIE_DESC =
            "HttpOnly refresh token cookie (name=refreshToken). Secure; Path=/; SameSite=Lax; Max-Age=1209600 (14 days) when set.";

    private static final String SET_COOKIE_REFRESH_DESC =
            "Sets refreshToken HttpOnly cookie: refreshToken=<token>; Path=/; Max-Age=1209600; HttpOnly; Secure; SameSite=Lax";

    private static final String SET_COOKIE_CLEAR_DESC =
            "Clears refreshToken cookie: refreshToken=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Lax";

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
    @Operation(operationId = "authHello", summary = "Health / hello probe")
    public ApiResponse<String> hello() {
        return ApiResponse.success("访问成功");
    }

    @PostMapping("/v1/auth/login")
    @Operation(operationId = "authLogin", summary = "Login and issue access token + refresh cookie")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login succeeded; refresh token is only in Set-Cookie",
                    headers = @Header(name = "Set-Cookie", description = SET_COOKIE_REFRESH_DESC,
                            schema = @Schema(type = "string")))
    })
    public ApiResponse<AuthResult> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "studentLogin",
                                    value = "{\"email\":\"student@example.com\",\"password\":\"Passw0rd1\",\"role\":\"USER\"}")))
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        Account account = toAccount(loginRequest);
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
    @Operation(operationId = "authRefreshToken", summary = "Rotate refresh cookie and return new access token")
    @Parameter(name = "refreshToken", in = ParameterIn.COOKIE, required = true,
            description = REFRESH_COOKIE_DESC, schema = @Schema(type = "string"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "New access token in body; rotated refresh token only in Set-Cookie",
                    headers = @Header(name = "Set-Cookie", description = SET_COOKIE_REFRESH_DESC,
                            schema = @Schema(type = "string")))
    })
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
    @Operation(operationId = "authLogout", summary = "Revoke refresh token and clear cookie")
    @Parameter(name = "refreshToken", in = ParameterIn.COOKIE, required = false,
            description = REFRESH_COOKIE_DESC, schema = @Schema(type = "string"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logged out; refresh cookie cleared",
                    headers = @Header(name = "Set-Cookie", description = SET_COOKIE_CLEAR_DESC,
                            schema = @Schema(type = "string")))
    })
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request);
        if (StrUtil.isNotBlank(refreshToken)) {
            refreshTokenService.deleteByToken(refreshToken);
        }
        clearRefreshTokenCookie(response);
        return ApiResponse.success();
    }

    @PostMapping("/v1/auth/register")
    @Operation(operationId = "authRegister", summary = "Register and issue access token + refresh cookie")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Registration succeeded; refresh token is only in Set-Cookie",
                    headers = @Header(name = "Set-Cookie", description = SET_COOKIE_REFRESH_DESC,
                            schema = @Schema(type = "string")))
    })
    public ApiResponse<AuthResult> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(
                                    name = "register",
                                    value = "{\"email\":\"student@example.com\",\"verificationCode\":\"123456\",\"password\":\"Passw0rd1\",\"name\":\"Student One\",\"username\":\"student1\",\"tenantId\":1}")))
            @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        if (request == null || StrUtil.isBlank(request.getPassword()) || StrUtil.isBlank(request.getEmail())
                || StrUtil.isBlank(request.getName()) || StrUtil.isBlank(request.getVerificationCode())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }

        AuthResult result = userService.register(request);
        setRefreshTokenCookie(response, result.getRefreshToken());
        return ApiResponse.success(result);
    }

    @PostMapping("/v1/auth/email-verifications/register")
    @Operation(operationId = "authSendRegisterEmailVerification", summary = "Send registration email verification code")
    public ApiResponse<Void> sendRegisterEmailVerification(
            @Parameter(description = "Email to verify", example = "student@example.com", required = true)
            String email) {
        userService.sendEmailVerificationCode(email, "register");
        return ApiResponse.success();
    }

    @PostMapping("/v1/auth/email-verifications/reset")
    @Operation(operationId = "authSendResetEmailVerification", summary = "Send password-reset email verification code")
    public ApiResponse<Void> sendResetEmailVerification(
            @Parameter(description = "Email to verify", example = "student@example.com", required = true)
            String email) {
        userService.sendEmailVerificationCode(email, "reset");
        return ApiResponse.success();
    }

    @Idempotent
    @PutMapping("/v1/auth/password")
    @Operation(operationId = "authUpdatePassword", summary = "Change password for the authenticated principal")
    public ApiResponse<Void> updatePassword(
            HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChangePasswordRequest.class),
                            examples = @ExampleObject(
                                    name = "changePassword",
                                    value = "{\"currentPassword\":\"OldPassw0rd\",\"newPassword\":\"NewPassw0rd1\"}")))
            @RequestBody ChangePasswordRequest body) {
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
    @Operation(operationId = "authResetPassword", summary = "Reset password with email verification code")
    public ApiResponse<Void> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PasswordResetRequest.class),
                            examples = @ExampleObject(
                                    name = "resetPassword",
                                    value = "{\"email\":\"student@example.com\",\"verificationCode\":\"123456\",\"newPassword\":\"NewPassw0rd1\"}")))
            @RequestBody PasswordResetRequest dto) {
        if (dto == null || StrUtil.isBlank(dto.getEmail()) || StrUtil.isBlank(dto.getNewPassword())
                || StrUtil.isBlank(dto.getVerificationCode())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        userService.resetPassword(dto);
        return ApiResponse.success();
    }

    private static Account toAccount(LoginRequest loginRequest) {
        Account account = new Account();
        if (loginRequest != null) {
            account.setEmail(loginRequest.getEmail());
            account.setPassword(loginRequest.getPassword());
            account.setRole(loginRequest.getRole());
        }
        return account;
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
