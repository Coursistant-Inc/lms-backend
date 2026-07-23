package com.coursistant.lms.module.auth.session.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
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
    public Result hello() {
        return Result.success("访问成功"); // Access successful
    }

    @PostMapping("/login")
    public Result login(@RequestBody Account account, HttpServletResponse response) {
        if (ObjectUtil.isEmpty(account.getEmail()) || ObjectUtil.isEmpty(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        Account dbAccount = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            dbAccount = adminService.login(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            dbAccount = userService.login(account);
        }

        // TODO: Error handling
        if (dbAccount == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
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
            // 1. 确保 RocketChat 用户存在
            rocketChatAuthService.ensureUserExists(
                    dbAccount.getEmail(),
                    account.getPassword(),
                    dbAccount.getName()
            );

            // 2. 创建 RocketChat token
            Map<String, String> rcToken = rocketChatAuthService.createTokenForUser(dbAccount.getEmail());

            // 3. 保存到 dbAccount
            dbAccount.setRocketChatToken(rcToken.get("authToken"));
            dbAccount.setRocketChatUserId(rcToken.get("userId"));

            // System.out.println("✅ RocketChat token created for: " + dbAccount.getEmail());
            log.info("✅ RocketChat token created for: {}", dbAccount.getEmail());

        } catch (Exception e) {
            log.warn("⚠️ RocketChat login failed: {}", e.getMessage());
        }

        return Result.success(dbAccount);
    }

    @PostMapping("/refresh-token")
    public Result refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // IP rate limiting: max 10 requests per minute
        String rateLimitKey = "ratelimit:refresh:" + request.getRemoteAddr();
        Long count = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(rateLimitKey, 60, TimeUnit.SECONDS);
        }
        if (count != null && count > 10) {
            return Result.error("429", "Too many requests, please try again later");
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
            return Result.error(ResultCodeEnum.REFRESH_TOKEN_CHECK_ERROR);
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

        return Result.success(result.getAccessToken());
    }


    @PostMapping("/logout")
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        Integer userId = (Integer) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");

        refreshTokenService.deleteByUserId(userId, role);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).path("/")
                .maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return Result.success();
    }

    /**
     * 注册 // Register
     */
    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getPassword()) || StrUtil.isBlank(account.getEmail())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.register(account);
            account.setAccessToken(TokenUtils.createAccessToken(account.getId(), RoleEnum.ADMIN.name()));
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.register(account);
            account.setAccessToken(TokenUtils.createAccessToken(account.getId(), RoleEnum.USER.name()));
        }

        return Result.success(account);
    }

    /**
     * 发送邮箱验证码 // Send email verification code register
     */
    @PostMapping("/sendRegisterEmailVerification")
    public Result sendRegisterEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "register");
        return Result.success();
    }

    /**
     * 校验邮箱验证码 // Validate email verification code
     */
    @PostMapping("/validateRegisterEmailVerification")
    public Result validateRegisterEmailVerification(@RequestParam("email") String email,
                                                    @RequestParam("code") String code) {
        userService.validateEmailVerificationCode(email, code);
        return Result.success();
    }


    @PostMapping("/sendResetEmailVerification")
    public Result sendResetEmailVerification(String email) {
        userService.sendEmailVerificationCode(email, "reset");
        return Result.success();
    }


    /**
     * 修改密码 // Update password
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody PasswordDTO account) {
        if ("update".equals(account.getType())) {
            if (StrUtil.isBlank(account.getEmail()) || StrUtil.isBlank(account.getPassword())
                    || ObjectUtil.isEmpty(account.getNewPassword())) {
                return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
            }
        } else if ("reset".equals(account.getType())) {
            if (StrUtil.isBlank(account.getEmail()) || ObjectUtil.isEmpty(account.getNewPassword())) {
                return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
            }
        } else {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.updatePassword(account);
        }
        return Result.success();
    }

    @PostMapping("/resetPasswordValidation")
    public Result resetPasswordValidation(@RequestBody PasswordDTO account) {

        if (StrUtil.isBlank(account.getEmail()) || StrUtil.isBlank(account.getCode())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        String token = userService.resetPasswordValidation(account);

        return Result.success(token);
    }

    /**
     * 处理查询请求 // Handle query request
     */
    @RequiresPermission("chatbot:interact")
    @PostMapping(value = "/query", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result query(@RequestParam(value = "file", required = false) MultipartFile file,
                        @RequestParam("courseId") Integer courseId,
                        @RequestParam("query") String query,
                        @RequestParam("dialogueId") Integer dialogueId,
                        @RequestParam("userId") Integer userId,
                        @RequestHeader(value = "X-Timezone", required = false) String timezone) {

        String savedFilePath = "N/A";

        try {
            if (file != null && !file.isEmpty()) {
                // 1️⃣ 生成日期目录 // Generate date-based directory
                String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
                String baseDir = "/home/ubuntu/SpringBoot/saved_images/query_images"; // 设定存储路径 // Set storage path
                //String baseDir = "C:\\Users\\Charlottejas\\Desktop\\Jerry\\项目脚手架\\manager\\"; // 设定存储路径
                String uploadDir = baseDir + datePath + "/";
                File dir = new File(uploadDir);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + uploadDir);
                }

                // 2️⃣ 生成唯一文件名 // Generate a unique file name
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File destFile = new File(uploadDir + fileName);

                // 3️⃣ 先手动存储文件，避免 `Tomcat` 清除临时文件 // Manually store the file to prevent `Tomcat` from clearing temporary files
                file.transferTo(destFile);
                savedFilePath = destFile.getAbsolutePath();

                log.info("File saved at: {}", savedFilePath);
            }
        } catch (Exception e) {
            log.error("Error saving file: {}", e.getMessage());

        }

        // 记录请求日志，包含文件存储路径 // Log request, including file storage path
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
        return Result.success(re_query);
    }


}
