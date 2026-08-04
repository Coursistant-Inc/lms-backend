package com.coursistant.lms.module.auth.admin.service;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.module.auth.session.dto.ChangePasswordRequest;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.shared.web.Constants;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.auth.session.dto.AuthResult;
import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.shared.util.PasswordEncoderUtil;
import com.coursistant.lms.shared.util.PasswordValidator;
import com.coursistant.lms.shared.security.SessionInvalidationService;
import com.coursistant.lms.shared.security.TokenUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 管理员业务处理 // Administrator business logic handling
 **/
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private LoginGuardService loginGuardService;

    @Resource
    private SessionInvalidationService sessionInvalidationService;

    @Resource
    private IdentityAuditService identityAuditService;

    @Resource
    private com.coursistant.lms.module.auth.token.service.RefreshTokenService refreshTokenService;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource(name = "adminAllRedisTemplate")
    private RedisTemplate<String, Object> adminAllRedisTemplate;


    // 缓存过期时间（秒） // Cache expiration time (seconds)
    private static final long CACHE_EXPIRE_TIME = 300;

    /**
     * 清空 adminAll 数据库 // Clear the adminAll database
     */
    public void clearAdminAllCache() {
        Objects.requireNonNull(adminAllRedisTemplate.getConnectionFactory()).getConnection().flushDb();
        System.out.println("Cleared all data from adminAll database.");
    }



    /**
     * 新增 // Add a new admin
     */
    public void add(Admin admin) {
        Admin dbAdmin = adminMapper.selectByEmail(admin.getEmail());
        if (ObjectUtil.isNotNull(dbAdmin)) {
            throw new ApiException(ErrorType.USER_ALREADY_EXISTS, "Username Already Exists");
        }
        if (ObjectUtil.isEmpty(admin.getPassword())) {
            admin.setEncryptPassword(Constants.USER_DEFAULT_PASSWORD);
        } else {
            admin.setEncryptPassword(admin.getPassword());
        }
        if (ObjectUtil.isEmpty(admin.getName())) {
            admin.setName(admin.getUsername());
        }
        admin.setRole(RoleEnum.SYSTEM_ADMIN.name());
        adminMapper.insert(admin);
        // 清理相关缓存 // Clear related caches
        clearAdminAllCache();

    }

    /**
     * 删除 // Delete by ID
     */
    public void deleteById(Integer id) {
        adminMapper.deleteById(id);
        // 清理相关缓存 // Clear related caches
        clearAdminAllCache();

        generalRedisTemplate.delete("admin:" + id);
    }

    /**
     * 批量删除 // Batch delete
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            adminMapper.deleteById(id);
            // 清理相关缓存 // Clear related caches
            generalRedisTemplate.delete("admin:" + id);
        }
        // 清理相关缓存 // Clear related caches
        clearAdminAllCache();

    }

    /**
     * 修改 // Update admin by ID
     */
    public void updateById(Admin admin) {
        adminMapper.updateById(admin);
        // 清理相关缓存 // Clear related caches
        clearAdminAllCache();

        generalRedisTemplate.delete("admin:" + admin.getId());
        generalRedisTemplate.delete("admin:email:" + admin.getEmail());
    }

    /**
     * 根据ID查询 // Select admin by ID
     */
    public Admin selectById(Integer id) {
        String cacheKey = "admin:" + id;
        // 从 Redis 获取缓存 // Get from Redis cache
        Admin admin = (Admin) generalRedisTemplate.opsForValue().get(cacheKey);
        if (admin != null) {
            System.out.println("from cache");
            return admin; // 返回缓存数据 // Return cached data
        }
        // 如果缓存不存在，从数据库查询 // If cache does not exist, query from the database
        admin = adminMapper.selectById(id);
        if (ObjectUtil.isNull(admin)) {
            throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
        }
        // 将结果存入 Redis，并设置过期时间 // Store result in Redis with expiration time
        generalRedisTemplate.opsForValue().set(cacheKey, admin, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);

        return admin;
    }

    /**
     * 查询所有 // Select all admins
     */
    public List<Admin> selectAll(Admin admin) {
        // 构造缓存键 // Construct cache key
        String cacheKey = "admin:all";
        if (admin != null) {
            cacheKey += admin.toString();
        }

        // 从 Redis 获取缓存 // Get from Redis cache
        List<Admin> admins = (List<Admin>) adminAllRedisTemplate.opsForValue().get(cacheKey);
        if (admins != null) {
            System.out.println("from cache: " + cacheKey);
            return admins; // 返回缓存数据 // Return cached data
        }

        // 如果缓存不存在，从数据库查询 // If cache does not exist, query from the database
        admins = adminMapper.selectAll(admin);
        if (admins != null && !admins.isEmpty()) {
            // 将结果存入 Redis，并设置过期时间 // Store result in Redis with expiration time
            adminAllRedisTemplate.opsForValue().set(cacheKey, admins, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }

        return admins;
    }



    /**
     * SYSTEM_ADMIN login against the admin table.
     */
    public AuthResult login(Account account) {
        String email = AccountIdentityService.normalizeEmail(account.getEmail());
        loginGuardService.assertNotLocked(LoginGuardService.ACCOUNT_ADMIN, email);

        Admin dbAdmin = adminMapper.selectByEmail(email);
        if (ObjectUtil.isNull(dbAdmin)) {
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_ADMIN, email, "USER_NOT_FOUND");
        }
        if (dbAdmin.getStatus() != null && !AccountStatus.ACTIVE.name().equals(dbAdmin.getStatus())) {
            log.info("Login rejected: disabled admin id={}", dbAdmin.getId());
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_ADMIN, email, "ACCOUNT_DISABLED");
        }

        if (!PasswordEncoderUtil.matches(account.getPassword(), dbAdmin.getPassword())) {
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_ADMIN, email, "BAD_PASSWORD");
        }

        loginGuardService.clearOnSuccess(LoginGuardService.ACCOUNT_ADMIN, email);

        Integer authVersion = dbAdmin.getAuthVersion() == null ? 1 : dbAdmin.getAuthVersion();
        String accessToken = TokenUtils.createAccessToken(
                dbAdmin.getId(), RoleEnum.SYSTEM_ADMIN.name(), authVersion, null);
        String refreshToken = refreshTokenService.createAndStoreRefreshToken(
                dbAdmin.getId(), RoleEnum.SYSTEM_ADMIN.name());

        AuthResult result = new AuthResult();
        result.setUserId(dbAdmin.getId());
        result.setEmail(dbAdmin.getEmail());
        result.setName(dbAdmin.getName());
        result.setUsername(dbAdmin.getUsername());
        result.setRole(dbAdmin.getRole());
        result.setLevel(dbAdmin.getLevel());
        result.setAvatar(dbAdmin.getAvatar());
        result.setAccessToken(accessToken);
        result.setRefreshToken(refreshToken);
        return result;
    }


    /**
     * 注册 // Admin registration
     */
    public void register(Account account) {
        Admin admin = new Admin();
        BeanUtils.copyProperties(account, admin);
        add(admin);
        clearAdminAllCache();

    }

    /**
     * Change password for the authenticated SYSTEM_ADMIN principal.
     */
    @Transactional
    public void updatePasswordForPrincipal(Integer adminId, ChangePasswordRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()
                || request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "Parameter Missing");
        }
        Admin dbAdmin = adminMapper.selectById(adminId);
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new ApiException(ErrorType.USER_NOT_FOUND, "User Does Not Exist");
        }
        if (!PasswordEncoderUtil.matches(request.getCurrentPassword(), dbAdmin.getPassword())) {
            throw new ApiException(ErrorType.INVALID_PASSWORD, "Incorrect Original Password");
        }
        PasswordValidator.validate(request.getNewPassword());
        dbAdmin.setPassword(PasswordEncoderUtil.encodePassword(request.getNewPassword()));
        adminMapper.updateById(dbAdmin);
        adminMapper.incrementAuthVersion(dbAdmin.getId());
        sessionInvalidationService.invalidatePrincipal(dbAdmin.getId(), RoleEnum.SYSTEM_ADMIN.name());
        generalRedisTemplate.delete("admin:email:" + dbAdmin.getEmail());
        generalRedisTemplate.delete("admin:" + dbAdmin.getId());
        identityAuditService.writeSuccess(dbAdmin.getId(), RoleEnum.SYSTEM_ADMIN.name(), null,
                "CHANGE_PASSWORD", "ADMIN", dbAdmin.getId(), null,
                null, "{\"authVersion\":\"bumped\"}", null, null);
    }
}
