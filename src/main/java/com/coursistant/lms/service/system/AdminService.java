package com.coursistant.lms.service.system;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.entity.DTO.PasswordDTO;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.system.AdminMapper;
import com.coursistant.lms.common.Constants;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.common.enums.RoleEnum;
import com.coursistant.lms.entity.Account;
import com.coursistant.lms.entity.Admin;
import com.coursistant.lms.utils.PasswordEncoderUtil;
import com.coursistant.lms.utils.TokenUtils;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 管理员业务处理 // Administrator business logic handling
 **/
@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private RefreshTokenService refreshTokenService;

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
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(admin.getPassword())) {
            admin.setEncryptPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        if (ObjectUtil.isEmpty(admin.getName())) {
            admin.setName(admin.getUsername());
        }
        admin.setRole(RoleEnum.ADMIN.name());
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
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
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
     * 登录 // Admin login
     */
    public Account login(Account account) {

        // Redis 缓存键 // Redis cache keys
        String cacheKey = "admin:email:" + account.getEmail(); // 用户缓存键 // User cache key
        String loginAttemptsKey = "admin:login:attempts:" + account.getEmail(); // 登录尝试次数键 // Login attempt key
        String lockKey = "admin:login:lock:" + account.getEmail(); // 锁定状态键 // Lock status key

        Account dbAdmin;

        // 检查是否被锁定 // Check if the account is locked
        if (Boolean.TRUE.equals(generalRedisTemplate.hasKey(lockKey))) {
            throw new CustomException("6001", "Your account is locked. Please try again later.");
        }

        // 尝试从 Redis 缓存中获取用户信息 // Try to retrieve user information from Redis cache
        Account cachedAccount = (Account) generalRedisTemplate.opsForValue().get(cacheKey);
        if (cachedAccount != null) {
            dbAdmin = cachedAccount;
        }
        else {
            // 查询数据库 // Query the database if cache is not available
            dbAdmin = adminMapper.selectByEmail(account.getEmail());
        }
        // 如果用户不存在 // If the user does not exist
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        // 将用户信息存入 Redis 缓存，设置过期时间 // Store user information in Redis cache with expiration time
        generalRedisTemplate.opsForValue().set(cacheKey, dbAdmin, 3600, TimeUnit.SECONDS); // 缓存 1 小时 // Cache for 1 hour

        // 验证密码 // Validate password
        if (!PasswordEncoderUtil.matches(account.getPassword(), dbAdmin.getPassword())) {
            // 更新登录尝试次数 // Update login attempt count
            Integer attempts = (Integer) generalRedisTemplate.opsForValue().get(loginAttemptsKey);
            attempts = (attempts == null) ? 1 : attempts + 1;
            generalRedisTemplate.opsForValue().set(loginAttemptsKey, attempts, 15, TimeUnit.MINUTES); // 保存尝试次数 15 分钟 // Store attempts for 15 minutes

            // 判断锁定条件 // Check lock conditions
            if (attempts >= 6) {
                long lockTime = (attempts < 10) ? 60 : 600; // 第 6 次锁 1 分钟，超过 10 次锁 10 分钟 // Lock for 1 minute on 6th attempt, 10 minutes after 10 attempts
                generalRedisTemplate.opsForValue().set(lockKey, "LOCKED", lockTime, TimeUnit.SECONDS);
                throw new CustomException("6002", "Your account is locked. Please try again in " + (lockTime / 60) + " minutes.");
            }

            throw new CustomException("6003", "Invalid email or password. Remaining attempts: " + (6 - attempts));
        }

        // 登录成功后，清除登录尝试限制和锁定状态 // Upon successful login, clear login attempt restrictions and lock status
        generalRedisTemplate.delete(loginAttemptsKey);
        generalRedisTemplate.delete(lockKey);

        // 生成 Token // Generate authentication token
        String tokenData = dbAdmin.getId() + "-" + RoleEnum.ADMIN.name();
        String accessToken = TokenUtils.createAccessToken(tokenData);
        dbAdmin.setAccessToken(accessToken);

        String refreshToken=refreshTokenService.createAndStoreRefreshToken(dbAdmin.getId(),dbAdmin.getRole());
        dbAdmin.setRefreshToken(refreshToken);

        return dbAdmin;
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
     * 修改密码 // Update password
     */
    public void updatePassword(PasswordDTO account) {
        Admin dbAdmin = adminMapper.selectByEmail(account.getEmail());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if ("reset".equals(account.getType())) {
            if (!PasswordEncoderUtil.matches(account.getPassword(), dbAdmin.getPassword())) {
                throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
            }
        }
        // 加密新密码然后设置 // Encrypt new password and set it
        String encryptedNewPassword = PasswordEncoderUtil.encodePassword(account.getNewPassword());
        dbAdmin.setPassword(encryptedNewPassword);
        adminMapper.updateById(dbAdmin);

        generalRedisTemplate.delete("admin:email:" + account.getEmail());
        generalRedisTemplate.delete("admin:" + dbAdmin.getId());
    }
}
