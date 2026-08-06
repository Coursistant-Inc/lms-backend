package com.coursistant.lms.shared.security;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.RoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActorContextResolverTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private AdminMapper adminMapper;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ActorContextResolver resolver;

    @Test
    void systemAdmin_ok() {
        when(request.getAttribute(AuthzService.ATTR_USER_ID)).thenReturn(1);
        when(request.getAttribute(AuthzService.ATTR_USER_ROLE)).thenReturn(RoleEnum.SYSTEM_ADMIN.name());
        Admin admin = new Admin();
        admin.setId(1);
        admin.setStatus(AccountStatus.ACTIVE.name());
        when(adminMapper.selectById(1)).thenReturn(admin);

        ActorContext ctx = resolver.resolve(request);
        assertTrue(ctx.isSystemAdmin());
        assertNull(ctx.getTenantId());
    }

    @Test
    void disabledUser_unauthorized() {
        when(request.getAttribute(AuthzService.ATTR_USER_ID)).thenReturn(2);
        when(request.getAttribute(AuthzService.ATTR_USER_ROLE)).thenReturn(RoleEnum.USER.name());
        User user = new User();
        user.setId(2);
        user.setTenantId(1);
        user.setStatus(AccountStatus.DISABLED.name());
        user.setLevel("STUDENT");
        when(userMapper.selectById(2)).thenReturn(user);

        ApiException ex = assertThrows(ApiException.class, () -> resolver.resolve(request));
        assertEquals(ErrorType.UNAUTHORIZED, ex.getErrorType());
    }

    @Test
    void disabledTenant_unauthorized() {
        when(request.getAttribute(AuthzService.ATTR_USER_ID)).thenReturn(2);
        when(request.getAttribute(AuthzService.ATTR_USER_ROLE)).thenReturn(RoleEnum.USER.name());
        User user = new User();
        user.setId(2);
        user.setTenantId(1);
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setLevel("STUDENT");
        when(userMapper.selectById(2)).thenReturn(user);
        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setStatus(AccountStatus.DISABLED.name());
        when(tenantMapper.selectById(1)).thenReturn(tenant);

        ApiException ex = assertThrows(ApiException.class, () -> resolver.resolve(request));
        assertEquals(ErrorType.UNAUTHORIZED, ex.getErrorType());
    }

    @Test
    void tenantAdmin_hasTenant() {
        when(request.getAttribute(AuthzService.ATTR_USER_ID)).thenReturn(3);
        when(request.getAttribute(AuthzService.ATTR_USER_ROLE)).thenReturn(RoleEnum.TENANT_ADMIN.name());
        User user = new User();
        user.setId(3);
        user.setTenantId(9);
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setLevel("NOT_APPLICABLE");
        when(userMapper.selectById(3)).thenReturn(user);
        Tenant tenant = new Tenant();
        tenant.setId(9);
        tenant.setStatus(AccountStatus.ACTIVE.name());
        when(tenantMapper.selectById(9)).thenReturn(tenant);

        ActorContext ctx = resolver.resolve(request);
        assertTrue(ctx.isTenantAdmin());
        assertEquals(9, ctx.getTenantId());
    }
}
