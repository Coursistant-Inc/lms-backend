package com.coursistant.lms.module.auth.unit;

import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.auth.identity.service.ManagedUserService;
import com.coursistant.lms.module.auth.support.AuthFixture;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.AuthzService;
import com.coursistant.lms.shared.security.SessionInvalidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagedUserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserService userService;
    @Mock
    private AccountIdentityService accountIdentityService;
    @Mock
    private IdentityAuditService identityAuditService;
    @Mock
    private SessionInvalidationService sessionInvalidationService;
    @Mock
    private AuthzService authzService;

    @InjectMocks
    private ManagedUserService managedUserService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
    }

    @Test
    void createUser_tenantAdmin_cannotCreateSystemAdmin() {
        when(authzService.requireUserId(request)).thenReturn(11);
        when(authzService.requireRole(request)).thenReturn("TENANT_ADMIN");
        when(authzService.resolveActorTenantId(request)).thenReturn(1);
        when(authzService.isTenantAdmin(request)).thenReturn(true);

        var cmd = new ManagedUserService.CreateManagedUserCommand(
                "x@ex.com", "X", RoleEnum.SYSTEM_ADMIN.name(), null, 1);

        ApiException ex = assertThrows(ApiException.class,
                () -> managedUserService.createUser(request, cmd, false));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void createUser_tenantAdmin_usesActorTenantNotCmdTenant() {
        when(authzService.requireUserId(request)).thenReturn(11);
        when(authzService.requireRole(request)).thenReturn("TENANT_ADMIN");
        when(authzService.resolveActorTenantId(request)).thenReturn(1);
        when(authzService.isTenantAdmin(request)).thenReturn(true);

        var cmd = new ManagedUserService.CreateManagedUserCommand(
                "x@ex.com", "X", RoleEnum.USER.name(), "STUDENT", 2);

        doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99);
            return 1;
        }).when(userMapper).insert(any(User.class));

        Integer id = managedUserService.createUser(request, cmd, false);
        assertEquals(99, id);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getTenantId());
        verify(accountIdentityService).claimEmail(eq("x@ex.com"), eq(AccountIdentityService.PRINCIPAL_USER), eq(99));
    }

    @Test
    void createUser_systemScope_requiresSystemAdmin() {
        when(authzService.requireUserId(request)).thenReturn(11);
        when(authzService.requireRole(request)).thenReturn("TENANT_ADMIN");
        when(authzService.resolveActorTenantId(request)).thenReturn(1);
        doThrow(new ApiException(ErrorType.FORBIDDEN, "System admin required"))
                .when(authzService).requireSystemAdmin(request);

        var cmd = new ManagedUserService.CreateManagedUserCommand(
                "x@ex.com", "X", RoleEnum.USER.name(), "STUDENT", 1);
        ApiException ex = assertThrows(ApiException.class,
                () -> managedUserService.createUser(request, cmd, true));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void createUser_userActor_forbidden() {
        when(authzService.requireUserId(request)).thenReturn(21);
        when(authzService.requireRole(request)).thenReturn("USER");
        when(authzService.resolveActorTenantId(request)).thenReturn(1);
        when(authzService.isTenantAdmin(request)).thenReturn(false);

        var cmd = new ManagedUserService.CreateManagedUserCommand(
                "x@ex.com", "X", RoleEnum.USER.name(), "STUDENT", 1);
        ApiException ex = assertThrows(ApiException.class,
                () -> managedUserService.createUser(request, cmd, false));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
    }

    @Test
    void changeRole_cannotPromoteToSystemAdmin() {
        User target = AuthFixture.student(1);
        when(authzService.requireUserId(request)).thenReturn(1);
        when(authzService.requireRole(request)).thenReturn("SYSTEM_ADMIN");
        when(authzService.isSystemAdmin(request)).thenReturn(true);
        when(userService.selectById(21)).thenReturn(target);

        ApiException ex = assertThrows(ApiException.class,
                () -> managedUserService.changeRole(request, 21, RoleEnum.SYSTEM_ADMIN.name(), null));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void changeRole_cannotDemoteLastTenantAdmin() {
        User target = AuthFixture.tenantAdmin(1);
        when(authzService.requireUserId(request)).thenReturn(1);
        when(authzService.requireRole(request)).thenReturn("SYSTEM_ADMIN");
        when(authzService.isSystemAdmin(request)).thenReturn(true);
        when(userService.selectById(target.getId())).thenReturn(target);
        when(userService.selectAll(any(User.class))).thenReturn(List.of(target));

        ApiException ex = assertThrows(ApiException.class,
                () -> managedUserService.changeRole(request, target.getId(), RoleEnum.USER.name(), "STUDENT"));
        assertEquals(ErrorType.CONFLICT, ex.getErrorType());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void changeRole_tenantAdmin_cannotModifySelf() {
        User target = AuthFixture.tenantAdmin(1);
        when(authzService.requireUserId(request)).thenReturn(target.getId());
        when(authzService.requireRole(request)).thenReturn("TENANT_ADMIN");
        when(authzService.isSystemAdmin(request)).thenReturn(false);
        when(authzService.isTenantAdmin(request)).thenReturn(true);
        when(userService.selectById(target.getId())).thenReturn(target);
        doNothing().when(authzService).requireTenantAdminOrSystem(request, 1);

        ApiException ex = assertThrows(ApiException.class,
                () -> managedUserService.changeRole(request, target.getId(), RoleEnum.USER.name(), "STUDENT"));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
    }

    @Test
    void changeRole_success_invalidatesSessionsAndWritesAudit() {
        User target = AuthFixture.student(1);
        when(authzService.requireUserId(request)).thenReturn(1);
        when(authzService.requireRole(request)).thenReturn("SYSTEM_ADMIN");
        when(authzService.isSystemAdmin(request)).thenReturn(true);
        when(authzService.resolveActorTenantId(request)).thenReturn(null);
        when(userService.selectById(21)).thenReturn(target);

        managedUserService.changeRole(request, 21, RoleEnum.TENANT_ADMIN.name(), null);

        verify(userMapper).updateById(argThat(u -> RoleEnum.TENANT_ADMIN.name().equals(u.getRole())));
        verify(sessionInvalidationService, atLeastOnce()).invalidatePrincipal(eq(21), anyString());
        verify(identityAuditService).writeSuccess(eq(1), eq("SYSTEM_ADMIN"), isNull(),
                eq("CHANGE_ROLE"), eq("USER"), eq(21), eq(1), anyString(), anyString(), isNull(), eq("127.0.0.1"));
    }
}
