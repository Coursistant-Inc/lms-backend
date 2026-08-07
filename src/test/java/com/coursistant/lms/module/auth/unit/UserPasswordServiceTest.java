package com.coursistant.lms.module.auth.unit;

import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.identity.entity.AccountIdentity;
import com.coursistant.lms.module.auth.identity.repository.AccountIdentityMapper;
import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.auth.session.dto.ChangePasswordRequest;
import com.coursistant.lms.module.auth.session.dto.PasswordResetRequest;
import com.coursistant.lms.module.auth.session.service.EmailVerificationService;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.module.auth.support.AuthFixture;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.SessionInvalidationService;
import com.coursistant.lms.shared.util.EmailUtil;
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
class UserPasswordServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private AdminMapper adminMapper;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginGuardService loginGuardService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private AccountIdentityService accountIdentityService;
    @Mock
    private AccountIdentityMapper accountIdentityMapper;
    @Mock
    private IdentityAuditService identityAuditService;
    @Mock
    private SessionInvalidationService sessionInvalidationService;
    @Mock
    private RedisTemplate<String, Object> generalRedisTemplate;
    @Mock
    private EmailUtil emailUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void changePassword_usesPrincipalId_notBodyEmail() {
        User user = AuthFixture.student(1);
        when(userMapper.selectById(21)).thenReturn(user);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(AuthFixture.PASSWORD_PLAIN);
        req.setNewPassword("NewPassw0rd");

        userService.updatePasswordForPrincipal(21, req);

        verify(userMapper).selectById(21);
        verify(userMapper, never()).selectByEmail(anyString());
        verify(userMapper).incrementAuthVersion(21);
        verify(sessionInvalidationService).invalidatePrincipal(eq(21), eq("USER"));
    }

    @Test
    void resetPassword_routesViaAccountIdentityToUser() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("student-1@example.com");
        req.setVerificationCode("123456");
        req.setNewPassword("NewPassw0rd");
        AccountIdentity identity = new AccountIdentity();
        identity.setPrincipalType(AccountIdentityService.PRINCIPAL_USER);
        identity.setPrincipalId(21);
        when(accountIdentityMapper.selectByEmail("student-1@example.com")).thenReturn(identity);
        when(userMapper.selectById(21)).thenReturn(AuthFixture.student(1));

        userService.resetPassword(req);

        verify(emailVerificationService).requireConsumeSuccess("reset", "student-1@example.com", "123456");
        verify(userMapper).incrementAuthVersion(21);
        verify(adminMapper, never()).updateById(any());
        verify(sessionInvalidationService).invalidatePrincipal(eq(21), eq("USER"));
    }

    @Test
    void resetPassword_wrongCurrentNotApplicable_codeConsumedThenDbMissingFails() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("missing@example.com");
        req.setVerificationCode("123456");
        req.setNewPassword("NewPassw0rd");
        when(accountIdentityMapper.selectByEmail("missing@example.com")).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> userService.resetPassword(req));
        assertEquals(ErrorType.BAD_REQUEST, ex.getErrorType());
        verify(emailVerificationService).requireConsumeSuccess(eq("reset"), eq("missing@example.com"), eq("123456"));
    }

    @Test
    void resetPassword_weakPassword_doesNotConsumeCode() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("student-1@example.com");
        req.setVerificationCode("123456");
        req.setNewPassword("short");

        ApiException ex = assertThrows(ApiException.class, () -> userService.resetPassword(req));
        assertEquals(ErrorType.INVALID_PASSWORD_FORMAT, ex.getErrorType());
        verify(emailVerificationService, never()).requireConsumeSuccess(anyString(), anyString(), anyString());
    }
}
