package com.coursistant.lms.module.auth.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.auth.support.AuthFixture;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.AccessTokenAuthService;
import com.coursistant.lms.shared.security.JwtParserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessTokenAuthServiceTest {

    @Mock
    private JwtParserUtil jwtParserUtil;
    @Mock
    private AdminService adminService;
    @Mock
    private UserService userService;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private DecodedJWT jwt;
    @Mock
    private Claim userIdClaim;
    @Mock
    private Claim roleClaim;
    @Mock
    private Claim typeClaim;
    @Mock
    private Claim authVersionClaim;
    @Mock
    private Claim tenantSecClaim;

    @InjectMocks
    private AccessTokenAuthService accessTokenAuthService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        ReflectionTestUtils.setField(accessTokenAuthService, "minIssuedAtConfig", "");
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private void stubJwt(String role, int userId, String type, Integer authVersion, Integer tenantSec) {
        when(jwtParserUtil.verify("tok")).thenReturn(jwt);
        when(jwt.getClaim("type")).thenReturn(typeClaim);
        when(jwt.getClaim("userId")).thenReturn(userIdClaim);
        when(jwt.getClaim("role")).thenReturn(roleClaim);
        when(jwt.getClaim("authVersion")).thenReturn(authVersionClaim);
        when(jwt.getClaim("tenantSecurityVersion")).thenReturn(tenantSecClaim);
        when(jwt.getSubject()).thenReturn(String.valueOf(userId));
        when(jwt.getIssuedAt()).thenReturn(new Date());
        when(typeClaim.asString()).thenReturn(type);
        when(userIdClaim.asInt()).thenReturn(userId);
        when(roleClaim.asString()).thenReturn(role);
        when(authVersionClaim.asInt()).thenReturn(authVersion);
        if (tenantSec == null) {
            when(tenantSecClaim.isNull()).thenReturn(true);
        } else {
            when(tenantSecClaim.isNull()).thenReturn(false);
            when(tenantSecClaim.asInt()).thenReturn(tenantSec);
        }
    }

    @Test
    void missingBearer_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer(null, request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void legacyAdminRole_rejected() {
        stubJwt("ADMIN", 1, "access", 1, null);
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void typeMissing_rejected() {
        stubJwt("USER", 21, null, 1, 1);
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void subMismatch_rejected() {
        stubJwt("USER", 21, "access", 1, 1);
        when(jwt.getSubject()).thenReturn("99");
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void systemAdmin_active_ok() {
        stubJwt("SYSTEM_ADMIN", 1, "access", 1, null);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(adminService.selectById(1)).thenReturn(AuthFixture.activeSystemAdmin());

        var principal = accessTokenAuthService.authenticateBearer("Bearer tok", request);
        assertEquals(1, principal.userId());
        assertEquals("SYSTEM_ADMIN", principal.role());
        assertEquals(1, request.getAttribute("userId"));
    }

    @Test
    void user_authVersionMismatch_rejected() {
        stubJwt("USER", 21, "access", 1, 1);
        User user = AuthFixture.student(1);
        user.setAuthVersion(2);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(userService.selectById(21)).thenReturn(user);

        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void user_disabled_rejected() {
        stubJwt("USER", 41, "access", 1, 1);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(userService.selectById(41)).thenReturn(AuthFixture.disabledUser(1));

        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void user_tenantDisabled_rejected() {
        stubJwt("USER", 21, "access", 1, 1);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(userService.selectById(21)).thenReturn(AuthFixture.student(1));
        when(tenantMapper.selectById(1)).thenReturn(AuthFixture.disabledTenant(1));

        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void user_tenantSecurityVersionMismatch_rejected() {
        stubJwt("USER", 21, "access", 1, 1);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(userService.selectById(21)).thenReturn(AuthFixture.student(1));
        var tenant = AuthFixture.activeTenant(1);
        tenant.setSecurityVersion(9);
        when(tenantMapper.selectById(1)).thenReturn(tenant);

        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void principalCacheMiss_fallsBackToDb_andCaches() {
        stubJwt("SYSTEM_ADMIN", 1, "access", 1, null);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(adminService.selectById(1)).thenReturn(AuthFixture.activeSystemAdmin());

        accessTokenAuthService.authenticateBearer("Bearer tok", request);
        verify(valueOperations).set(eq("auth:principal:admin:1"), eq("1"), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void redisCacheError_stillLoadsFromDb() {
        stubJwt("SYSTEM_ADMIN", 1, "access", 1, null);
        when(stringRedisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        when(adminService.selectById(1)).thenReturn(AuthFixture.activeSystemAdmin());

        assertDoesNotThrow(() -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
    }

    @Test
    void cutoverRejectsOldIat() {
        ReflectionTestUtils.setField(accessTokenAuthService, "minIssuedAtConfig", "2099-01-01T00:00:00Z");
        stubJwt("SYSTEM_ADMIN", 1, "access", 1, null);
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void adminUserSameNumericId_useDistinctCacheKeys() {
        stubJwt("SYSTEM_ADMIN", AuthFixture.SHARED_NUMERIC_ID, "access", 1, null);
        when(stringRedisTemplate.hasKey("auth:principal:admin:42")).thenReturn(false);
        when(adminService.selectById(42)).thenReturn(AuthFixture.systemAdminWithSameIdAsUser());
        accessTokenAuthService.authenticateBearer("Bearer tok", request);
        verify(valueOperations).set(eq("auth:principal:admin:42"), eq("1"), eq(5L), eq(TimeUnit.MINUTES));
        verify(userService, never()).selectById(anyInt());
    }

    @Test
    void emptyBearer_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer ", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void refreshType_rejected() {
        stubJwt("USER", 21, "refresh", 1, 1);
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void tenantAdmin_active_ok() {
        stubJwt("TENANT_ADMIN", 11, "access", 1, 1);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(userService.selectById(11)).thenReturn(AuthFixture.tenantAdmin(1));
        when(tenantMapper.selectById(1)).thenReturn(AuthFixture.activeTenant(1));

        var principal = accessTokenAuthService.authenticateBearer("Bearer tok", request);
        assertEquals(11, principal.userId());
        assertEquals("TENANT_ADMIN", principal.role());
    }

    @Test
    void redisAndDbBothFail_rejected() {
        stubJwt("SYSTEM_ADMIN", 1, "access", 1, null);
        when(stringRedisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        when(adminService.selectById(1)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void userMissingTenant_rejected() {
        stubJwt("USER", 21, "access", 1, 1);
        User user = AuthFixture.student(1);
        user.setTenantId(null);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(userService.selectById(21)).thenReturn(user);

        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void principalCacheHit_loadsFromDbWithoutRecacheRequirement() {
        stubJwt("SYSTEM_ADMIN", 1, "access", 1, null);
        when(stringRedisTemplate.hasKey("auth:principal:admin:1")).thenReturn(true);
        when(adminService.selectById(1)).thenReturn(AuthFixture.activeSystemAdmin());

        accessTokenAuthService.authenticateBearer("Bearer tok", request);
        verify(adminService).selectById(1);
    }

    @Test
    void malformedBearerPrefix_rejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Token tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }

    @Test
    void unknownRole_rejected() {
        stubJwt("SUPERUSER", 1, "access", 1, null);
        ApiException ex = assertThrows(ApiException.class,
                () -> accessTokenAuthService.authenticateBearer("Bearer tok", request));
        assertEquals(ErrorType.INVALID_TOKEN, ex.getErrorType());
    }
}
