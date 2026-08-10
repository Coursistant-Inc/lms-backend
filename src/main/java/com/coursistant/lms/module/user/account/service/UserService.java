package com.coursistant.lms.module.user.account.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.module.auth.session.dto.AuthResult;
import com.coursistant.lms.module.auth.session.dto.ChangePasswordRequest;
import com.coursistant.lms.module.auth.session.dto.PasswordResetRequest;
import com.coursistant.lms.module.auth.session.service.EmailVerificationService;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.util.EmailUtil;
import com.coursistant.lms.shared.util.PasswordValidator;

import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.user.account.dto.RegisterRequest;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.identity.entity.AccountIdentity;
import com.coursistant.lms.module.auth.identity.repository.AccountIdentityMapper;
import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.user.profile.AvatarUrlBuilder;
import com.coursistant.lms.shared.util.PasswordEncoderUtil;
import com.coursistant.lms.shared.security.SessionInvalidationService;
import com.coursistant.lms.shared.security.TokenUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User???? // User business processing
 **/
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private RefreshTokenService refreshTokenService;

    @Resource
    private LoginGuardService loginGuardService;

    @Resource
    private EmailVerificationService emailVerificationService;

    @Resource
    private AccountIdentityService accountIdentityService;

    @Resource
    private AccountIdentityMapper accountIdentityMapper;

    @Resource
    private IdentityAuditService identityAuditService;

    @Resource
    private SessionInvalidationService sessionInvalidationService;

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
     * Public registration. Forces USER + STUDENT + tenantId=1. Consumes verification code atomically.
     */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (request == null || StrUtil.isBlank(request.getEmail()) || StrUtil.isBlank(request.getVerificationCode())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        String email = AccountIdentityService.normalizeEmail(request.getEmail());

        // Static validations before consuming the code (anti-enumeration: existence check is after consume).
        PasswordValidator.validate(request.getPassword());
        if (StrUtil.isBlank(request.getName())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Display name is required");
        }
        // Public registration always binds to tenant 1 (ignore client tenantId for role/level).
        requirePublicRegistrationTenant(1);
        Tenant tenant = tenantMapper.selectById(1);
        if (tenant == null || (tenant.getStatus() != null && !AccountStatus.ACTIVE.name().equals(tenant.getStatus()))) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Registration failed");
        }

        emailVerificationService.requireConsumeSuccess(
                EmailVerificationService.TYPE_REGISTER, email, request.getVerificationCode());

        User existing = userMapper.selectByEmail(email);
        if (existing != null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Registration failed");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setTenantId(1);
        user.setLevel(LevelEnum.STUDENT.level);
        user.setRole(RoleEnum.USER.name());
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setAuthVersion(1);
        user.setEmailNotifications(true);
        if (StrUtil.isBlank(user.getUsername())) {
            user.setUsername(email.split("@")[0]);
        }
        user.setEncryptPassword(request.getPassword());
        userMapper.insert(user);
        accountIdentityService.claimEmail(email, AccountIdentityService.PRINCIPAL_USER, user.getId());

        Integer authVersion = user.getAuthVersion() == null ? 1 : user.getAuthVersion();
        Integer tenantSecurityVersion = tenant.getSecurityVersion() == null ? 1 : tenant.getSecurityVersion();
        String accessToken = TokenUtils.createAccessToken(
                user.getId(), RoleEnum.USER.name(), authVersion, tenantSecurityVersion);
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

    /** Drop Redis principal cache after direct mapper updates (e.g. disable). */
    public void evictUserCache(Integer id, String email) {
        if (id != null) {
            generalRedisTemplate.delete("user:" + id);
        }
        if (email != null && !email.isBlank()) {
            generalRedisTemplate.delete("user:email:" + email);
        }
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
     * User / TENANT_ADMIN login against the user table.
     */
    public AuthResult login(Account account) {
        String email = AccountIdentityService.normalizeEmail(account.getEmail());
        loginGuardService.assertNotLocked(LoginGuardService.ACCOUNT_USER, email);

        User dbUser = userMapper.selectByEmail(email);
        if (ObjectUtil.isNull(dbUser)) {
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, email, "USER_NOT_FOUND");
        }
        if (dbUser.getStatus() != null && !AccountStatus.ACTIVE.name().equals(dbUser.getStatus())) {
            log.info("Login rejected: disabled user id={}", dbUser.getId());
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, email, "ACCOUNT_DISABLED");
        }
        Tenant tenant = dbUser.getTenantId() == null ? null : tenantMapper.selectById(dbUser.getTenantId());
        if (tenant == null
                || (tenant.getStatus() != null && !AccountStatus.ACTIVE.name().equals(tenant.getStatus()))) {
            log.info("Login rejected: inactive tenant userId={} tenantId={}", dbUser.getId(), dbUser.getTenantId());
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, email, "TENANT_DISABLED");
        }

        if (!PasswordEncoderUtil.matches(account.getPassword(), dbUser.getPassword())) {
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, email, "BAD_PASSWORD");
        }

        loginGuardService.clearOnSuccess(LoginGuardService.ACCOUNT_USER, email);

        try {
            Integer authVersion = dbUser.getAuthVersion() == null ? 1 : dbUser.getAuthVersion();
            Integer tenantSecurityVersion = tenant.getSecurityVersion() == null ? 1 : tenant.getSecurityVersion();
            String accessToken = TokenUtils.createAccessToken(
                    dbUser.getId(), dbUser.getRole(), authVersion, tenantSecurityVersion);
            String refreshToken = refreshTokenService.createAndStoreRefreshToken(dbUser.getId(), dbUser.getRole());
            return toAuthResult(dbUser, accessToken, refreshToken);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorType.TOKEN_CREATION_FAILED, "Error When Creating Token");
        }
    }

    /**
     * Change password for the authenticated USER / TENANT_ADMIN principal.
     */
    @Transactional
    public void updatePasswordForPrincipal(Integer userId, ChangePasswordRequest request) {
        if (request == null || StrUtil.isBlank(request.getCurrentPassword())
                || StrUtil.isBlank(request.getNewPassword())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        User dbUser = userMapper.selectById(userId);
        if (ObjectUtil.isNull(dbUser)) {
            throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
        }
        if (!PasswordEncoderUtil.matches(request.getCurrentPassword(), dbUser.getPassword())) {
            throw new ApiException(ErrorType.INVALID_PASSWORD, "Incorrect Original Password");
        }
        PasswordValidator.validate(request.getNewPassword());
        dbUser.setPassword(PasswordEncoderUtil.encodePassword(request.getNewPassword()));
        dbUser.setMustChangePassword(false);
        userMapper.updateById(dbUser);
        userMapper.incrementAuthVersion(dbUser.getId());
        sessionInvalidationService.invalidatePrincipal(dbUser.getId(), dbUser.getRole());
        generalRedisTemplate.delete("user:email:" + dbUser.getEmail());
        generalRedisTemplate.delete("user:" + dbUser.getId());
        identityAuditService.writeSuccess(dbUser.getId(), dbUser.getRole(), dbUser.getTenantId(),
                "CHANGE_PASSWORD", "USER", dbUser.getId(), dbUser.getTenantId(),
                null, "{\"authVersion\":\"bumped\"}", null, null);
    }

    /**
     * Reset password via verification code + account_identity (admin or user table).
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (request == null || StrUtil.isBlank(request.getEmail())
                || StrUtil.isBlank(request.getVerificationCode())
                || StrUtil.isBlank(request.getNewPassword())) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        String email = AccountIdentityService.normalizeEmail(request.getEmail());
        PasswordValidator.validate(request.getNewPassword());
        emailVerificationService.requireConsumeSuccess(
                EmailVerificationService.TYPE_RESET, email, request.getVerificationCode());

        AccountIdentity identity = accountIdentityMapper.selectByEmail(email);
        if (identity == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Password reset failed");
        }

        String encoded = PasswordEncoderUtil.encodePassword(request.getNewPassword());
        if (AccountIdentityService.PRINCIPAL_ADMIN.equals(identity.getPrincipalType())) {
            Admin admin = adminMapper.selectById(identity.getPrincipalId());
            if (admin == null) {
                throw new ApiException(ErrorType.BAD_REQUEST, "Password reset failed");
            }
            admin.setPassword(encoded);
            adminMapper.updateById(admin);
            adminMapper.incrementAuthVersion(admin.getId());
            sessionInvalidationService.invalidatePrincipal(admin.getId(), RoleEnum.SYSTEM_ADMIN.name());
            generalRedisTemplate.delete("admin:email:" + email);
            generalRedisTemplate.delete("admin:" + admin.getId());
            identityAuditService.writeSuccess(admin.getId(), RoleEnum.SYSTEM_ADMIN.name(), null,
                    "RESET_PASSWORD", "ADMIN", admin.getId(), null,
                    null, "{\"authVersion\":\"bumped\"}", null, null);
            return;
        }

        User dbUser = userMapper.selectById(identity.getPrincipalId());
        if (dbUser == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Password reset failed");
        }
        dbUser.setPassword(encoded);
        dbUser.setMustChangePassword(false);
        userMapper.updateById(dbUser);
        userMapper.incrementAuthVersion(dbUser.getId());
        sessionInvalidationService.invalidatePrincipal(dbUser.getId(), dbUser.getRole());
        generalRedisTemplate.delete("user:email:" + email);
        generalRedisTemplate.delete("user:" + dbUser.getId());
        identityAuditService.writeSuccess(dbUser.getId(), dbUser.getRole(), dbUser.getTenantId(),
                "RESET_PASSWORD", "USER", dbUser.getId(), dbUser.getTenantId(),
                null, "{\"authVersion\":\"bumped\"}", null, null);
    }

    /**
     * ?????? // Select teacher users
     */
    public List<User> selectTeachers() {
        return userMapper.selectTeachers();
    }

    /**
     * Send email verification code (register / reset). Forgot-password is not gated by login lock.
     */
    public void sendEmailVerificationCode(String email, String type) {
        if (ObjectUtil.isEmpty(email)) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        email = AccountIdentityService.normalizeEmail(email);
        emailVerificationService.assertCanSend(type, email);

        boolean shouldSend;
        if (EmailVerificationService.TYPE_REGISTER.equals(type)) {
            shouldSend = userMapper.selectByEmail(email) == null
                    && accountIdentityMapper.selectByEmail(email) == null;
        } else {
            shouldSend = accountIdentityMapper.selectByEmail(email) != null;
        }
        if (!shouldSend) {
            return;
        }

        String verificationCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        emailVerificationService.storeCode(type, email, verificationCode);

        String subject;
        String content;
        if (EmailVerificationService.TYPE_REGISTER.equals(type)) {
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
        if (user instanceof User u) {
            result.setMustChangePassword(Boolean.TRUE.equals(u.getMustChangePassword()));
        }
        return result;
    }
}
