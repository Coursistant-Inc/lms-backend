package com.coursistant.lms.controller.system;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.coursistant.lms.service.system.RefreshTokenService;
import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.common.enums.RoleEnum;
import com.coursistant.lms.entity.Account;
import com.coursistant.lms.entity.DTO.PasswordDTO;
import com.coursistant.lms.entity.Query;
import com.coursistant.lms.service.chat.CoursistanceService;
import com.coursistant.lms.service.system.AdminService;
import com.coursistant.lms.service.user.UserService;
import com.coursistant.lms.utils.TokenUtils;
import com.coursistant.lms.utils.TimeZoneUtils;
import com.coursistant.lms.annotation.RequiresPermission;
import com.coursistant.lms.v2.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.coursistant.lms.service.groupchat.RocketChatAuthService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
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


    @GetMapping("/")
    public Result hello() {
        return Result.success("访问成功"); // Access successful
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Account>> login(@RequestBody Account account, HttpServletResponse response) {
        if (ObjectUtil.isEmpty(account.getEmail()) || ObjectUtil.isEmpty(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(4001, "Parameter missing"));
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
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(4001, "Parameter missing"));
        }

        var standardJwt = TokenUtils.createStandardAccessToken(dbAccount.getId(), dbAccount.getRole());
        var accCookie = new Cookie("accessToken", standardJwt);
        accCookie.setHttpOnly(true);
        accCookie.setSecure(true);
        accCookie.setPath("/");
        accCookie.setMaxAge(2 * 60 * 60);
        response.addCookie(accCookie);

        if (ObjectUtil.isNotEmpty(dbAccount.getRefreshToken())) {
            Cookie cookie = new Cookie("refreshToken", dbAccount.getRefreshToken());
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 30);
            response.addCookie(cookie);
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

        return ResponseEntity.ok(
                ApiResponse.success("Login success", dbAccount)
        );
    }

    @PostMapping("/refresh-token")
    public Result refreshToken(HttpServletRequest request) {
        // ✅ 从 Cookie 中读取 refreshToken
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

        // 校验并生成新的 accessToken
        String newAccessToken = refreshTokenService.getNewAccessToken(refreshToken);
        return Result.success(newAccessToken);
    }


    // @PostMapping("/loginWithLinkedIn")
    // public Result loginWithLinkedIn() {
    //     String link= linkedInAuthService.returnUrl();
    //     return Result.success(link);
    // }

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
            // 生成token // Generate token
            String tokenData = account.getId() + "-" + RoleEnum.ADMIN.name();
            String token = TokenUtils.createAccessToken(tokenData);
            account.setAccessToken(token);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.register(account);
            // 生成token // Generate token
            String tokenData = account.getId() + "-" + RoleEnum.USER.name();
            String token = TokenUtils.createAccessToken(tokenData);
            account.setAccessToken(token);
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
