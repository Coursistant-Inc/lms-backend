package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.identity.entity.AccountIdentity;
import com.coursistant.lms.module.auth.identity.repository.AccountIdentityMapper;
import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuthPersistenceIT extends AuthIntegrationTestBase {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private AccountIdentityMapper accountIdentityMapper;
    @Autowired
    private AccountIdentityService accountIdentityService;
    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    void accountIdentity_uniqueNormalizedEmail() {
        String email = dataFactory.uniqueEmail("persist");
        dataFactory.createStudent(1, email);
        assertThrows(Exception.class, () -> accountIdentityService.claimEmail(
                "  " + email.toUpperCase() + "  ", AccountIdentityService.PRINCIPAL_USER, 99999));
    }

    @Test
    void systemAdmin_hasNullTenantAndNotApplicableSemantics() {
        Admin admin = dataFactory.createSystemAdmin(dataFactory.uniqueEmail("sys"));
        assertEquals(RoleEnum.SYSTEM_ADMIN.name(), admin.getRole());
        assertNull(admin.getPhone()); // no tenant on admin table
        User student = dataFactory.createStudent(1, dataFactory.uniqueEmail("stu"));
        assertEquals(1, student.getTenantId());
        assertEquals(LevelEnum.STUDENT.level, student.getLevel());
        User tadmin = dataFactory.createTenantAdmin(1, dataFactory.uniqueEmail("tadm"));
        assertEquals(LevelEnum.NOT_APPLICABLE.level, tadmin.getLevel());
        assertNotNull(tadmin.getTenantId());
    }

    @Test
    void concurrentSameEmail_atMostOneIdentity() throws Exception {
        String email = dataFactory.uniqueEmail("race");
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger();
        Runnable r = () -> {
            try {
                start.await();
                User u = new User();
                u.setTenantId(1);
                u.setEmail(email);
                u.setUsername("u" + Thread.currentThread().threadId());
                u.setName("race");
                u.setRole(RoleEnum.USER.name());
                u.setLevel(LevelEnum.STUDENT.level);
                u.setStatus("ACTIVE");
                u.setAuthVersion(1);
                u.setMustChangePassword(false);
                u.setEncryptPassword(AuthTestDataFactory.PASSWORD_PLAIN);
                userMapper.insert(u);
                accountIdentityService.claimEmail(email, AccountIdentityService.PRINCIPAL_USER, u.getId());
                success.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                done.countDown();
            }
        };
        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        t1.start();
        t2.start();
        start.countDown();
        done.await();
        assertTrue(success.get() <= 1);
        AccountIdentity id = accountIdentityMapper.selectByEmail(
                AccountIdentityService.normalizeEmail(email));
        assertNotNull(id);
    }

    @Test
    void refreshSession_sessionIdUniqueAndForUpdate() {
        User u = dataFactory.createStudent(1, dataFactory.uniqueEmail("sess"));
        RefreshToken rt = new RefreshToken();
        rt.setSessionId("session-abc");
        rt.setUserId(u.getId());
        rt.setToken("token-abc");
        rt.setRole(RoleEnum.USER.name());
        rt.setExpireTime(new Date(System.currentTimeMillis() + 86_400_000));
        refreshTokenMapper.insert(rt);

        RefreshToken locked = refreshTokenMapper.selectBySessionIdForUpdate("session-abc");
        assertNotNull(locked);
        assertEquals("token-abc", locked.getToken());

        RefreshToken dup = new RefreshToken();
        dup.setSessionId("session-abc");
        dup.setUserId(u.getId());
        dup.setToken("token-dup");
        dup.setRole(RoleEnum.USER.name());
        dup.setExpireTime(new Date(System.currentTimeMillis() + 86_400_000));
        assertThrows(DuplicateKeyException.class, () -> refreshTokenMapper.insert(dup));
    }

    @Test
    void maxFiveSessions_perUserRoleQueryable() {
        User u = dataFactory.createStudent(1, dataFactory.uniqueEmail("five"));
        for (int i = 0; i < 5; i++) {
            RefreshToken rt = new RefreshToken();
            rt.setSessionId("s-" + i + "-" + u.getId());
            rt.setUserId(u.getId());
            rt.setToken("t-" + i + "-" + u.getId());
            rt.setRole(RoleEnum.USER.name());
            rt.setExpireTime(new Date(System.currentTimeMillis() + 86_400_000));
            refreshTokenMapper.insert(rt);
        }
        List<RefreshToken> list = refreshTokenMapper.selectByUserIdAndRoleOrderByCreateTime(u.getId(), RoleEnum.USER.name());
        assertEquals(5, list.size());
    }
}
