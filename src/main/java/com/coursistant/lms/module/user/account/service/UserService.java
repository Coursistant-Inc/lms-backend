package com.coursistant.lms.module.user.account.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.module.auth.session.dto.AuthResult;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.util.EmailUtil;
import com.coursistant.lms.shared.util.PasswordValidator;

import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.auth.admin.dto.PasswordDTO;
import com.coursistant.lms.module.user.account.dto.RegisterRequest;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.profile.AvatarUrlBuilder;
import com.coursistant.lms.shared.util.PasswordEncoderUtil;
import com.coursistant.lms.shared.security.TokenUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * User???? // User business processing
 **/
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private RefreshTokenService refreshTokenService;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource
    private EmailUtil emailUtil;

    private static final long CACHE_EXPIRE_TIME = 300;

    /**
     * Add a new user (Admin / internal). {@code tenantId} is required and must exist.
     */
    public void add(User user) {
        User dbUser = userMapper.selectByEmail(user.getEmail());
        if (ObjectUtil.isNotNull(dbUser)) {
            throw new ApiException(ErrorType.USER_ALREADY_EXISTS, "Username Already Exists");
        }

        if (ObjectUtil.isEmpty(user.getPassword())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        requireExistingTenant(user.getTenantId());
        PasswordValidator.validate(user.getPassword());
        user.setEncryptPassword(user.getPassword());

        if (ObjectUtil.isEmpty(user.getName())) {
            user.setName(user.getUsername());
        }
        if (ObjectUtil.isEmpty(user.getLevel())) {
            user.setLevel(LevelEnum.STUDENT.level);
        }
        if (user.getEmailNotifications() == null) {
            user.setEmailNotifications(true);
        }

        user.setRole(RoleEnum.USER.name());
        userMapper.insert(user);
    }

    /**
     * Public registration. {@code tenantId} is required and must be {@code 1}.
     */
    public AuthResult register(RegisterRequest request) {
        if (request == null || StrUtil.isBlank(request.getEmail())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        String email = request.getEmail().trim().toLowerCase();

        String verifiedKey = "email:verified:register:" + email;
        if (!Boolean.TRUE.equals(generalRedisTemplate.hasKey(verifiedKey))) {
            throw new ApiException(ErrorType.INVALID_VERIFICATION_CODE, "Email not verified");
        }

        PasswordValidator.validate(request.getPassword());

        if (StrUtil.isBlank(request.getName())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Display name is required");
        }

        requirePublicRegistrationTenant(request.getTenantId());

        User existing = userMapper.selectByEmail(email);
        if (existing != null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Registration failed");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setTenantId(request.getTenantId());
        user.setLevel(LevelEnum.STUDENT.level);
        user.setRole(RoleEnum.USER.name());
        user.setEmailNotifications(true);
        if (StrUtil.isBlank(user.getUsername())) {
            user.setUsername(email.split("@")[0]);
        }
        user.setEncryptPassword(request.getPassword());
        userMapper.insert(user);

        generalRedisTemplate.delete(verifiedKey);

        String accessToken = TokenUtils.createAccessToken(user.getId(), RoleEnum.USER.name());
        String refreshToken = refreshTokenService.createAndStoreRefreshToken(user.getId(), RoleEnum.USER.name());
        return toAuthResult(user, accessToken, refreshToken);
    }

    /**
     * Update user by ID. Rejects any attempt to set {@code tenantId} via this path.
     */
    public void updateById(User user) {
        if (user.getTenantId() != null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "tenantId cannot be changed via this API; use PATCH /v2/admin/users/{id}/tenant");
        }
        userMapper.updateById(user);
        generalRedisTemplate.delete("user:" + user.getId());
        if (user.getEmail() != null) {
            generalRedisTemplate.delete("user:email:" + user.getEmail());
        }
    }

    /**
     * Admin-only: change a user's tenant when they have no course/enrollment links.
     */
    public User changeTenant(Integer userId, Integer tenantId) {
        User existing = selectById(userId);
        requireExistingTenant(tenantId);
        if (Objects.equals(existing.getTenantId(), tenantId)) {
            return existing;
        }
        if (enrollmentMapper.countByUserId(userId) > 0
                || courseMapper.countByInstructorOrCreator(userId) > 0) {
            throw new ApiException(ErrorType.USER_TENANT_CHANGE_BLOCKED);
        }
        userMapper.updateTenantId(userId, tenantId);
        generalRedisTemplate.delete("user:" + userId);
        if (existing.getEmail() != null) {
            generalRedisTemplate.delete("user:email:" + existing.getEmail());
        }
        return selectById(userId);
    }

    public void requireExistingTenant(Integer tenantId) {
        if (tenantId == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "tenantId is required");
        }
        if (tenantMapper.selectById(tenantId) == null) {
            throw new ApiException(ErrorType.TENANT_NOT_FOUND);
        }
    }

    public void requirePublicRegistrationTenant(Integer tenantId) {
        requireExistingTenant(tenantId);
        if (!Integer.valueOf(1).equals(tenantId)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Public registration only allows tenantId=1");
        }
    }

    /**
     * Delete user by ID
     */
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
        generalRedisTemplate.delete("user:" + id);
    }

    /**
     * Batch delete users
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            userMapper.deleteById(id);
            generalRedisTemplate.delete("user:" + id);
        }
    }

    /**
     * Select user by ID
     */
    public User selectById(Integer id) {
        String cacheKey = "user:" + id;

        User user = (User) generalRedisTemplate.opsForValue().get(cacheKey);
        if (user != null) {
            return user;
        }

        user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user)) {
            throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
        }

        generalRedisTemplate.opsForValue().set(cacheKey, user, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return user;
    }

    /**
     * ???? // Select all users
     */
    @SuppressWarnings("unchecked")
    public List<User> selectAll(User user) {
        String cacheKey = "user:all";
        if (user != null) {
            cacheKey += user.toString();
        }

        List<User> users = (List<User>) generalRedisTemplate.opsForValue().get(cacheKey);
        if (users != null) {
            return users;
        }

        users = userMapper.selectAll(user);
        if (users != null && !users.isEmpty()) {
            generalRedisTemplate.opsForValue().set(cacheKey, users, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return users;
    }

    /**
     * ?????? // Batch select users by IDs
     */
    @SuppressWarnings("unchecked")
    public List<User> selectUsersByIds(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        String cacheKey = "users:batch:" + userIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        List<User> users = (List<User>) generalRedisTemplate.opsForValue().get(cacheKey);
        if (users != null) {
            return users;
        }

        users = userMapper.selectUsersByIds(userIds);
        if (users == null) {
            users = new ArrayList<>();
        }

        if (!users.isEmpty()) {
            generalRedisTemplate.opsForValue().set(cacheKey, users, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }

        return users;
    }

    /**
     * ?? / User login
     */
    public AuthResult login(Account account) {
        String loginAttemptsKey = "user:login:attempts:" + account.getEmail();
        String lockKey = "user:login:lock:" + account.getEmail();

        if (Boolean.TRUE.equals(generalRedisTemplate.hasKey(lockKey))) {
            throw new ApiException(ErrorType.ACCOUNT_LOCKED, "Your account is locked. Please try again later.");
        }

        // Always load from DB for password verification. Redis must not cache credentials:
        // Account.password is WRITE_ONLY, so Jackson Redis serialization drops the hash.
        User dbUser = userMapper.selectByEmail(account.getEmail());

        if (ObjectUtil.isNull(dbUser)) {
            throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
        }

        if (!PasswordEncoderUtil.matches(account.getPassword(), dbUser.getPassword())) {
            Integer attempts = (Integer) generalRedisTemplate.opsForValue().get(loginAttemptsKey);
            attempts = (attempts == null) ? 1 : attempts + 1;
            generalRedisTemplate.opsForValue().set(loginAttemptsKey, attempts, 15, TimeUnit.MINUTES);

            if (attempts >= 6) {
                long lockTime = (attempts < 10) ? 60 : 600;
                generalRedisTemplate.opsForValue().set(lockKey, "LOCKED", lockTime, TimeUnit.SECONDS);
                throw new ApiException(ErrorType.ACCOUNT_LOCKED,
                        "Your account is locked. Please try again in " + (lockTime / 60) + " minutes.");
            }

            throw new ApiException(ErrorType.INVALID_CREDENTIALS,
                    "Invalid email or password. Remaining attempts: " + (6 - attempts));
        }

        generalRedisTemplate.delete(loginAttemptsKey);
        generalRedisTemplate.delete(lockKey);

        try {
            String accessToken = TokenUtils.createAccessToken(dbUser.getId(), RoleEnum.USER.name());
            String refreshToken = refreshTokenService.createAndStoreRefreshToken(dbUser.getId(), dbUser.getRole());
            return toAuthResult(dbUser, accessToken, refreshToken);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorType.TOKEN_CREATION_FAILED, "Error When Creating Token: " + e.getMessage());
        }
    }

    /**
     * Validate email verification code and set verified mark.
     */
    public void validateEmailVerificationCode(String email, String code, String type) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(code) || StrUtil.isBlank(type)) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        email = email.trim().toLowerCase();

        String attemptsKey = "email:verification:attempts:" + type + ":" + email;
        Integer attempts = toInteger(generalRedisTemplate.opsForValue().get(attemptsKey));
        if (attempts != null && attempts >= 5) {
            throw new ApiException(ErrorType.VERIFICATION_ATTEMPTS_EXCEEDED);
        }

        String redisKey = "email:verification:" + type + ":" + email;
        String cachedCode = (String) generalRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null) {
            throw new ApiException(ErrorType.VERIFICATION_CODE_EXPIRED);
        }
        if (!cachedCode.equals(code)) {
            int next = (attempts == null) ? 1 : attempts + 1;
            generalRedisTemplate.opsForValue().set(attemptsKey, next, 10, TimeUnit.MINUTES);
            throw new ApiException(ErrorType.INVALID_VERIFICATION_CODE);
        }

        generalRedisTemplate.delete(redisKey);
        generalRedisTemplate.delete(attemptsKey);
        generalRedisTemplate.opsForValue().set(
                "email:verified:" + type + ":" + email, "true", 15, TimeUnit.MINUTES);
    }

    /**
     * ???? Change Password (logged-in user updating password)
     */
    public void updatePassword(PasswordDTO account) {
        User dbUser = userMapper.selectByEmail(account.getEmail());
        if (ObjectUtil.isNull(dbUser)) {
            throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
        }
        if (!PasswordEncoderUtil.matches(account.getPassword(), dbUser.getPassword())) {
            throw new ApiException(ErrorType.INVALID_PASSWORD, "Incorrect Original Password");
        }

        PasswordValidator.validate(account.getNewPassword());
        String encryptedNewPassword = PasswordEncoderUtil.encodePassword(account.getNewPassword());
        dbUser.setPassword(encryptedNewPassword);
        dbUser.setMustChangePassword(false);
        userMapper.updateById(dbUser);

        refreshTokenService.deleteByUserId(dbUser.getId(), dbUser.getRole());

        generalRedisTemplate.delete("user:email:" + account.getEmail());
        generalRedisTemplate.delete("user:" + dbUser.getId());
    }

    /**
     * Reset password after email verification mark is set.
     */
    public void resetPassword(String email, String newPassword) {
        email = email.trim().toLowerCase();

        String verifiedKey = "email:verified:reset:" + email;
        if (!Boolean.TRUE.equals(generalRedisTemplate.hasKey(verifiedKey))) {
            throw new ApiException(ErrorType.INVALID_VERIFICATION_CODE, "Email not verified");
        }

        PasswordValidator.validate(newPassword);

        User dbUser = userMapper.selectByEmail(email);
        if (dbUser == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Password reset failed");
        }

        dbUser.setPassword(PasswordEncoderUtil.encodePassword(newPassword));
        dbUser.setMustChangePassword(false);
        userMapper.updateById(dbUser);

        refreshTokenService.deleteByUserId(dbUser.getId(), dbUser.getRole());

        generalRedisTemplate.delete(verifiedKey);
        generalRedisTemplate.delete("user:email:" + email);
        generalRedisTemplate.delete("user:" + dbUser.getId());
    }

    /**
     * ?????? // Select teacher users
     */
    public List<User> selectTeachers() {
        return userMapper.selectTeachers();
    }

    /**
     * ??????? // Send email verification code
     */
    public void sendEmailVerificationCode(String email, String type) {
        if (ObjectUtil.isEmpty(email)) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        email = email.trim().toLowerCase();

        if (!"register".equals(type) && !"reset".equals(type)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Invalid request data");
        }

        String cooldownKey = "email:verification:cooldown:" + type + ":" + email;
        if (Boolean.TRUE.equals(generalRedisTemplate.hasKey(cooldownKey))) {
            throw new ApiException(ErrorType.VERIFICATION_RESEND_COOLDOWN);
        }

        String hourlyKey = "email:verification:hourly:" + type + ":" + email;
        Integer hourlyCount = toInteger(generalRedisTemplate.opsForValue().get(hourlyKey));
        if (hourlyCount != null && hourlyCount >= 5) {
            throw new ApiException(ErrorType.VERIFICATION_HOURLY_LIMIT);
        }

        // Set cooldown + hourly counter BEFORE existence check (anti side-channel)
        generalRedisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);
        if (hourlyCount == null) {
            generalRedisTemplate.opsForValue().set(hourlyKey, 1, 1, TimeUnit.HOURS);
        } else {
            generalRedisTemplate.opsForValue().set(hourlyKey, hourlyCount + 1, 1, TimeUnit.HOURS);
        }

        User dbUser = userMapper.selectByEmail(email);
        if ("register".equals(type) && dbUser != null) {
            return;
        }
        if ("reset".equals(type) && dbUser == null) {
            return;
        }

        String verificationCode = String.format("%06d", new Random().nextInt(1000000));
        String redisKey = "email:verification:" + type + ":" + email;
        generalRedisTemplate.opsForValue().set(redisKey, verificationCode, 10, TimeUnit.MINUTES);

        String subject;
        String content;
        if ("register".equals(type)) {
            subject = "Registration Verification Code";
            content = "Dear User,\n\n"
                    + "Thank you for registering with Coursistant. Your verification code is: "
                    + verificationCode + ".\n\n"
                    + "This code is valid for 10 minutes. Please enter it promptly to complete your registration.\n\n"
                    + "If you did not request this code, please ignore this email.\n\n"
                    + "Best regards,\n"
                    + "Coursistant Team";
        } else {
            subject = "Password Reset Verification Code";
            content = "Dear User,\n\n"
                    + "You have requested to reset your password. Your verification code is: "
                    + verificationCode + ".\n\n"
                    + "This code is valid for 10 minutes. Please use it promptly to complete your password reset.\n\n"
                    + "If you did not request this code, please ignore this email.\n\n"
                    + "Best regards,\n"
                    + "Coursistant Team";
        }

        emailUtil.sendEmail(email, subject, content);
    }

    public String getUserLevel(Integer id) {
        return userMapper.selectUserLevelById(id);
    }

    public void markPasswordChanged(Integer id) {
        userMapper.updateMustChangePassword(id, false);
    }

    private AuthResult toAuthResult(Account user, String accessToken, String refreshToken) {
        AuthResult result = new AuthResult();
        result.setUserId(user.getId());
        result.setEmail(user.getEmail());
        result.setName(user.getName());
        result.setUsername(user.getUsername());
        result.setRole(user.getRole());
        result.setLevel(user.getLevel());
        result.setAvatar(AvatarUrlBuilder.buildStatic(user.getId(), user.getAvatar()));
        result.setAccessToken(accessToken);
        result.setRefreshToken(refreshToken);
        return result;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
