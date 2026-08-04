package com.coursistant.lms.module.auth.support;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.util.PasswordEncoderUtil;

/**
 * Shared fixtures for Auth unit tests. IDs are intentional overlaps for cache-collision cases.
 * Time-dependent TTL/lock behaviour is asserted via Redis mocks (no wall-clock waits).
 * JWT iat/exp tests use fixed offsets from System time in UTC-safe Date APIs.
 */
public final class AuthFixture {

    public static final String PASSWORD_PLAIN = "Passw0rd1";
    public static final Integer SHARED_NUMERIC_ID = 42;
    /** Fixed reference instant for deterministic JWT/cutoff assertions (UTC). */
    public static final java.time.Instant FIXED_UTC = java.time.Instant.parse("2026-07-28T12:00:00Z");

    private AuthFixture() {
    }

    public static Admin activeSystemAdmin() {
        Admin a = new Admin();
        a.setId(1);
        a.setEmail("sysadmin@example.com");
        a.setUsername("sysadmin");
        a.setName("System Admin");
        a.setRole(RoleEnum.SYSTEM_ADMIN.name());
        a.setStatus(AccountStatus.ACTIVE.name());
        a.setAuthVersion(1);
        a.setPassword(PasswordEncoderUtil.encodePassword(PASSWORD_PLAIN));
        return a;
    }

    public static Admin disabledSystemAdmin() {
        Admin a = activeSystemAdmin();
        a.setId(2);
        a.setEmail("disabled-admin@example.com");
        a.setStatus(AccountStatus.DISABLED.name());
        return a;
    }

    public static Admin systemAdminWithSameIdAsUser() {
        Admin a = activeSystemAdmin();
        a.setId(SHARED_NUMERIC_ID);
        a.setEmail("admin-same-id@example.com");
        return a;
    }

    public static Tenant activeTenant(int id) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setName("Tenant " + id);
        t.setTimezone("America/Los_Angeles");
        t.setStatus(AccountStatus.ACTIVE.name());
        t.setSecurityVersion(1);
        return t;
    }

    public static Tenant disabledTenant(int id) {
        Tenant t = activeTenant(id);
        t.setStatus(AccountStatus.DISABLED.name());
        return t;
    }

    public static User tenantAdmin(int tenantId) {
        User u = new User();
        u.setId(10 + tenantId);
        u.setTenantId(tenantId);
        u.setEmail("tenant-admin-" + tenantId + "@example.com");
        u.setUsername("tadmin" + tenantId);
        u.setName("Tenant Admin " + tenantId);
        u.setRole(RoleEnum.TENANT_ADMIN.name());
        u.setLevel(LevelEnum.NOT_APPLICABLE.level);
        u.setStatus(AccountStatus.ACTIVE.name());
        u.setAuthVersion(1);
        u.setMustChangePassword(false);
        u.setPassword(PasswordEncoderUtil.encodePassword(PASSWORD_PLAIN));
        return u;
    }

    public static User student(int tenantId) {
        User u = new User();
        u.setId(20 + tenantId);
        u.setTenantId(tenantId);
        u.setEmail("student-" + tenantId + "@example.com");
        u.setUsername("student" + tenantId);
        u.setName("Student " + tenantId);
        u.setRole(RoleEnum.USER.name());
        u.setLevel(LevelEnum.STUDENT.level);
        u.setStatus(AccountStatus.ACTIVE.name());
        u.setAuthVersion(1);
        u.setMustChangePassword(false);
        u.setPassword(PasswordEncoderUtil.encodePassword(PASSWORD_PLAIN));
        return u;
    }

    public static User instructor(int tenantId) {
        User u = student(tenantId);
        u.setId(30 + tenantId);
        u.setEmail("instructor-" + tenantId + "@example.com");
        u.setUsername("instructor" + tenantId);
        u.setName("Instructor " + tenantId);
        u.setLevel(LevelEnum.INSTRUCTOR.level);
        return u;
    }

    public static User disabledUser(int tenantId) {
        User u = student(tenantId);
        u.setId(40 + tenantId);
        u.setEmail("disabled-user-" + tenantId + "@example.com");
        u.setStatus(AccountStatus.DISABLED.name());
        return u;
    }

    public static User mustChangePasswordUser(int tenantId) {
        User u = student(tenantId);
        u.setId(50 + tenantId);
        u.setEmail("mcp-" + tenantId + "@example.com");
        u.setMustChangePassword(true);
        return u;
    }

    public static User userWithSameIdAsAdmin(int tenantId) {
        User u = student(tenantId);
        u.setId(SHARED_NUMERIC_ID);
        u.setEmail("user-same-id@example.com");
        return u;
    }

    public static String emailWithWhitespaceAndCase(String email) {
        return "  " + email.toUpperCase() + "  ";
    }
}
