package com.coursistant.lms.module.auth.unit;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.auth.session.dto.ChangePasswordRequest;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.module.auth.support.AuthFixture;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.SessionInvalidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPasswordServiceTest {

    @Mock
    private AdminMapper adminMapper;
    @Mock
    private LoginGuardService loginGuardService;
    @Mock
    private SessionInvalidationService sessionInvalidationService;
    @Mock
    private IdentityAuditService identityAuditService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private RedisTemplate<String, Object> generalRedisTemplate;
    @Mock
    private RedisTemplate<String, Object> adminAllRedisTemplate;

    @InjectMocks
    private AdminService adminService;

    @Test
    void updatePassword_wrongCurrent_doesNotPersist() {
        Admin admin = AuthFixture.activeSystemAdmin();
        when(adminMapper.selectById(1)).thenReturn(admin);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("WrongPass1");
        req.setNewPassword("NewPassw0rd");

        ApiException ex = assertThrows(ApiException.class,
                () -> adminService.updatePasswordForPrincipal(1, req));
        assertEquals(ErrorType.INVALID_PASSWORD, ex.getErrorType());
        verify(adminMapper, never()).updateById(any());
        verify(adminMapper, never()).incrementAuthVersion(anyInt());
        verify(sessionInvalidationService, never()).invalidatePrincipal(anyInt(), anyString());
    }

    @Test
    void updatePassword_success_bumpsAuthVersionAndInvalidatesSessions() {
        Admin admin = AuthFixture.activeSystemAdmin();
        when(adminMapper.selectById(1)).thenReturn(admin);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(AuthFixture.PASSWORD_PLAIN);
        req.setNewPassword("NewPassw0rd");

        adminService.updatePasswordForPrincipal(1, req);

        verify(adminMapper).updateById(any(Admin.class));
        verify(adminMapper).incrementAuthVersion(1);
        verify(sessionInvalidationService).invalidatePrincipal(eq(1), eq("SYSTEM_ADMIN"));
        verify(identityAuditService).writeSuccess(eq(1), eq("SYSTEM_ADMIN"), isNull(),
                eq("CHANGE_PASSWORD"), eq("ADMIN"), eq(1), isNull(), isNull(), anyString(), isNull(), isNull());
        verify(generalRedisTemplate).delete("admin:1");
    }

    @Test
    void updatePassword_weakNewPassword_rejected() {
        Admin admin = AuthFixture.activeSystemAdmin();
        when(adminMapper.selectById(1)).thenReturn(admin);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(AuthFixture.PASSWORD_PLAIN);
        req.setNewPassword("short");

        ApiException ex = assertThrows(ApiException.class,
                () -> adminService.updatePasswordForPrincipal(1, req));
        assertEquals(ErrorType.INVALID_PASSWORD_FORMAT, ex.getErrorType());
        verify(adminMapper, never()).incrementAuthVersion(anyInt());
    }
}
