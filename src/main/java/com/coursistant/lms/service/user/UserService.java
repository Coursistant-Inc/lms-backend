package com.coursistant.lms.service.user;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.service.system.RefreshTokenService;
import com.coursistant.lms.utils.EmailUtil;
import com.coursistant.lms.common.Constants;
import com.coursistant.lms.common.enums.LevelEnum;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.common.enums.RoleEnum;
import com.coursistant.lms.entity.Account;
import com.coursistant.lms.entity.DTO.PasswordDTO;
import com.coursistant.lms.entity.User;
import com.coursistant.lms.mapper.user.UserMapper;
import com.coursistant.lms.utils.PasswordEncoderUtil;
import com.coursistant.lms.utils.TokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * User业务处理 // User business processing
 **/
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RefreshTokenService refreshTokenService;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource(name = "userAllRedisTemplate")
    private RedisTemplate<String, Object> userAllRedisTemplate;


    @Resource
    private EmailUtil emailUtil; // 注入 EmailUtil // Inject EmailUtil

    // 缓存过期时间（秒） // Cache expiration time (seconds)
    private static final long CACHE_EXPIRE_TIME = 300;

    /**
     * 清空 userAll 数据库 // Clear the userAll database
     */
    public void clearUserAllCache() {
        Objects.requireNonNull(userAllRedisTemplate.getConnectionFactory()).getConnection().flushDb();
    }



    /**
     * 新增 // Add a new user
     */
    public void add(User user) {
        User dbUser = userMapper.selectByEmail(user.getEmail());
        if (ObjectUtil.isNotNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(user.getPassword())) {
            user.setEncryptPassword(Constants.USER_DEFAULT_PASSWORD);
        } else {
            user.setEncryptPassword(user.getPassword());
        }
        if (ObjectUtil.isEmpty(user.getName())) {
            user.setName(user.getUsername());
        }
        if (ObjectUtil.isEmpty(user.getLevel())) {
            user.setLevel(LevelEnum.STUDENT.level);
        }

        user.setRole(RoleEnum.USER.name());
        userMapper.insert(user);

        // 清理相关缓存 // Clear related caches
        clearUserAllCache();
    }

    /**
     * 删除 // Delete user by ID
     */
    public void deleteById(Integer id) {
        userMapper.deleteById(id);

        // 清理相关缓存 // Clear related caches
        clearUserAllCache();
        generalRedisTemplate.delete("user:" + id);
    }

    /**
     * 批量删除 // Batch delete users
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            userMapper.deleteById(id);
            generalRedisTemplate.delete("user:" + id);
        }
        clearUserAllCache();
    }

    /**
     * 修改 // Update user by ID
     */
    public void updateById(User user) {
        userMapper.updateById(user);

        // 清理相关缓存 // Clear related caches
        clearUserAllCache();
        generalRedisTemplate.delete("user:" + user.getId());
        generalRedisTemplate.delete("user:email:" + user.getEmail());
    }

    /**
     * 根据ID查询 // Select user by ID
     */
    public User selectById(Integer id) {
        String cacheKey = "user:" + id;

        // 从 Redis 获取缓存 // Get from Redis cache
        User user = (User) generalRedisTemplate.opsForValue().get(cacheKey);
        if (user != null) {
            return user;
        }

        // 如果缓存不存在，从数据库查询 // If cache does not exist, query from the database
        user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        // 将结果存入 Redis，并设置过期时间 // Store result in Redis with expiration time
        generalRedisTemplate.opsForValue().set(cacheKey, user, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return user;
    }

    /**
     * 查询所有 // Select all users
     */
    public List<User> selectAll(User user) {
        String cacheKey = "user:all";
        if (user != null) {
            cacheKey += user.toString();
        }

        // 从 Redis 获取缓存 // Get from Redis cache
        List<User> users = (List<User>) userAllRedisTemplate.opsForValue().get(cacheKey);
        if (users != null) {
            System.out.println("from cache: " + cacheKey);
            return users;
        }

        // 如果缓存不存在，从数据库查询 // If cache does not exist, query from the database
        users = userMapper.selectAll(user);
        if (users != null && !users.isEmpty()) {
            userAllRedisTemplate.opsForValue().set(cacheKey, users, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return users;
    }



    /**
     * 登录 // User login
     */
    public Account login(Account account) {
        String cacheKey = "user:email:" + account.getEmail();
        String loginAttemptsKey = "user:login:attempts:" + account.getEmail();
        String lockKey = "user:login:lock:" + account.getEmail();

        Account dbUser;

        if (Boolean.TRUE.equals(generalRedisTemplate.hasKey(lockKey))) {
            throw new CustomException("6001", "Your account is locked. Please try again later.");
        }

        Account cachedAccount = (Account) generalRedisTemplate.opsForValue().get(cacheKey);
        if (cachedAccount != null) {
            dbUser = cachedAccount;
        } else {
            dbUser = userMapper.selectByEmail(account.getEmail());
        }

        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }

        generalRedisTemplate.opsForValue().set(cacheKey, dbUser, 3600, TimeUnit.SECONDS);

        if (!PasswordEncoderUtil.matches(account.getPassword(), dbUser.getPassword())) {
            Integer attempts = (Integer) generalRedisTemplate.opsForValue().get(loginAttemptsKey);
            attempts = (attempts == null) ? 1 : attempts + 1;
            generalRedisTemplate.opsForValue().set(loginAttemptsKey, attempts, 15, TimeUnit.MINUTES);

            if (attempts >= 6) {
                long lockTime = (attempts < 10) ? 60 : 600;
                generalRedisTemplate.opsForValue().set(lockKey, "LOCKED", lockTime, TimeUnit.SECONDS);
                throw new CustomException("6002", "Your account is locked. Please try again in " + (lockTime / 60) + " minutes.");
            }

            throw new CustomException("6003", "Invalid email or password. Remaining attempts: " + (6 - attempts));
        }

        generalRedisTemplate.delete(loginAttemptsKey);
        generalRedisTemplate.delete(lockKey);

        String tokenData = dbUser.getId() + "-" + RoleEnum.USER.name();
        String token = TokenUtils.createAccessToken(tokenData);
        dbUser.setAccessToken(token);

        String refreshToken=refreshTokenService.createAndStoreRefreshToken(dbUser.getId(),dbUser.getRole());
        dbUser.setRefreshToken(refreshToken);

        return dbUser;
    }


    /**
     * 注册 register
     */
    public void register(Account account) {
        User user = new User();
        BeanUtils.copyProperties(account, user);


        // check invitation code
        String invitation = user.getInvitation();
        if ("PZMWXN4UUO".equals(invitation)) {
            user.setInvitation("Local Student");
        } else if ("YK0AU47BZ1".equals(invitation)) {
            user.setInvitation("International Student");
        } else if ("OPH31E5TOK".equals(invitation)) {
            user.setInvitation("Developer");
        } else if ("Z4G2MZ1XO1".equals(invitation)) {
            user.setInvitation("Teaching Class");
        } else {
            throw new CustomException(ResultCodeEnum.INVITATION_NOT_EXIST_ERROR);
        }

        add(user);
        clearUserAllCache();
        generalRedisTemplate.delete("email:verification:register:" + account.getEmail());
    }

    /**
     * VERIFICATION_CODE
     */
    public void validateEmailVerificationCode(String email, String verificationCode) {
        String redisKey = "email:verification:register:" + email;
        String cachedCode = (String) generalRedisTemplate.opsForValue().get(redisKey);

        if (ObjectUtil.isEmpty(cachedCode) || !cachedCode.equals(verificationCode)) {
            throw new CustomException(ResultCodeEnum.VERIFICATION_CODE_ERROR);
        }
    }



    /**
     * 修改密码 Change Password
     */
    public void updatePassword(PasswordDTO account) {
        User dbUser = userMapper.selectByEmail(account.getEmail());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!"reset".equals(account.getType())){
            if (!PasswordEncoderUtil.matches(account.getPassword(), dbUser.getPassword())) {
                throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
            }
        }


        String encryptedNewPassword = PasswordEncoderUtil.encodePassword(account.getNewPassword());
        dbUser.setPassword(encryptedNewPassword);
        userMapper.updateById(dbUser);

        generalRedisTemplate.delete("user:email:" + account.getEmail());
        generalRedisTemplate.delete("user:" + dbUser.getId());
    }

    /**
     * 修改密码 reset Password
     */
    public String resetPasswordValidation(PasswordDTO account) {
        //check email
        String redisKey = "email:verification:" + "reset"+":"+account.getEmail();
        String cachedCode = (String) generalRedisTemplate.opsForValue().get(redisKey);

        if (ObjectUtil.isEmpty(cachedCode) || !cachedCode.equals(account.getCode())) {
            throw new CustomException(ResultCodeEnum.VERIFICATION_CODE_ERROR);
        }
        //check user exist or not
        User dbUser = userMapper.selectByEmail(account.getEmail());
        if (ObjectUtil.isNull(dbUser)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        String tokenData = dbUser.getId() + "-" + RoleEnum.USER.name();
        String token = TokenUtils.createAccessToken(tokenData);

        generalRedisTemplate.delete(redisKey);

        return token;
    }

    /**
     * 查询教师用户 // Select teacher users
     */
    public List<User> selectTeachers() {
        return userMapper.selectTeachers();
    }

    /**
     * 发送邮箱验证码 // Send email verification code
     */
    public void sendEmailVerificationCode(String email, String type) {
        email = email.trim().toLowerCase();
        if (ObjectUtil.isEmpty(email)) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        if ("register".equals(type)) {
            User dbUser = userMapper.selectByEmail(email);
            if (ObjectUtil.isNotNull(dbUser)) {
                throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
            }
        } else if ("reset".equals(type)) {
            User dbUser = userMapper.selectByEmail(email);
            if (ObjectUtil.isNull(dbUser)) {
                throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
            }
        } else {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR); // 类型错误
        }



        // 生成 6 位随机验证码 //Generate a 6-digit random verification code
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        // 存入 Redis，有效期 5 分钟 //Stored in Redis, valid for 5 minutes
        String redisKey = "email:verification:" + type + ":" + email;
        generalRedisTemplate.opsForValue().set(redisKey, verificationCode, 5, TimeUnit.MINUTES);

        // 根据类型构建邮件内容
        String subject;
        String content;

        if ("register".equals(type)) {
            subject = "Registration Verification Code";
            content = "Dear User,\n\n" +
                    "Thank you for registering with Coursistant. Your verification code is: " + verificationCode + ".\n\n" +
                    "This code is valid for 5 minutes. Please enter it promptly to complete your registration.\n\n" +
                    "If you did not request this code, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Coursistant Team";
        } else {
            subject = "Password Reset Verification Code";
            content = "Dear User,\n\n" +
                    "You have requested to reset your password. Your verification code is: " + verificationCode + ".\n\n" +
                    "This code is valid for 5 minutes. Please use it promptly to complete your password reset.\n\n" +
                    "If you did not request this code, please ignore this email.\n\n" +
                    "Best regards,\n" +
                    "Coursistant Team";
        }

        // 发送邮件
        emailUtil.sendEmail(email, subject, content);
    }
}
