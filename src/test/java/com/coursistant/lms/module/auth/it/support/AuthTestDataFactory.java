package com.coursistant.lms.module.auth.it.support;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthTestDataFactory {

    public static final String PASSWORD_PLAIN = "Passw0rd1!";

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final AccountIdentityService accountIdentityService;
    private final TenantMapper tenantMapper;

    public AuthTestDataFactory(UserMapper userMapper,
                               AdminMapper adminMapper,
                               AccountIdentityService accountIdentityService,
                               TenantMapper tenantMapper) {
        this.userMapper = userMapper;
        this.adminMapper = adminMapper;
        this.accountIdentityService = accountIdentityService;
        this.tenantMapper = tenantMapper;
    }

    public String uniqueEmail(String prefix) {
        return prefix + "+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    public Admin createSystemAdmin(String email) {
        Admin a = new Admin();
        a.setEmail(email);
        a.setUsername(email.split("@")[0]);
        a.setName("System Admin");
        a.setRole(RoleEnum.SYSTEM_ADMIN.name());
        a.setStatus(AccountStatus.ACTIVE.name());
        a.setAuthVersion(1);
        a.setEncryptPassword(PASSWORD_PLAIN);
        adminMapper.insert(a);
        accountIdentityService.claimEmail(email, AccountIdentityService.PRINCIPAL_ADMIN, a.getId());
        return adminMapper.selectById(a.getId());
    }

    public User createTenantAdmin(int tenantId, String email) {
        User u = baseUser(tenantId, email);
        u.setRole(RoleEnum.TENANT_ADMIN.name());
        u.setLevel(LevelEnum.NOT_APPLICABLE.level);
        userMapper.insert(u);
        accountIdentityService.claimEmail(email, AccountIdentityService.PRINCIPAL_USER, u.getId());
        return userMapper.selectById(u.getId());
    }

    public User createStudent(int tenantId, String email) {
        User u = baseUser(tenantId, email);
        u.setRole(RoleEnum.USER.name());
        u.setLevel(LevelEnum.STUDENT.level);
        userMapper.insert(u);
        accountIdentityService.claimEmail(email, AccountIdentityService.PRINCIPAL_USER, u.getId());
        return userMapper.selectById(u.getId());
    }

    public User createInstructor(int tenantId, String email) {
        User u = baseUser(tenantId, email);
        u.setRole(RoleEnum.USER.name());
        u.setLevel(LevelEnum.INSTRUCTOR.level);
        userMapper.insert(u);
        accountIdentityService.claimEmail(email, AccountIdentityService.PRINCIPAL_USER, u.getId());
        return userMapper.selectById(u.getId());
    }

    public Tenant requireTenant(int id) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) {
            throw new IllegalStateException("Tenant " + id + " missing from IT schema seed");
        }
        return t;
    }

    private static User baseUser(int tenantId, String email) {
        User u = new User();
        u.setTenantId(tenantId);
        u.setEmail(email);
        u.setUsername(email.split("@")[0]);
        u.setName("User " + email);
        u.setStatus(AccountStatus.ACTIVE.name());
        u.setAuthVersion(1);
        u.setMustChangePassword(false);
        u.setEncryptPassword(PASSWORD_PLAIN);
        return u;
    }
}
