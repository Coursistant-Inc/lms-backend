package com.coursistant.lms.module.auth.unit;

import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.identity.repository.AccountIdentityMapper;
import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.auth.session.service.EmailVerificationService;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.dto.RegisterRequest;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
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
class UserRegistrationServiceTest {

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
    void register_weakPassword_doesNotConsumeCode() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setVerificationCode("123456");
        req.setPassword("weak");
        req.setName("New");

        ApiException ex = assertThrows(ApiException.class, () -> userService.register(req));
        assertEquals(ErrorType.INVALID_PASSWORD_FORMAT, ex.getErrorType());
        verify(emailVerificationService, never()).requireConsumeSuccess(anyString(), anyString(), anyString());
    }

    @Test
    void register_emailConflict_consumesCodeThenBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@example.com");
        req.setVerificationCode("123456");
        req.setPassword("Test12345");
        req.setName("Taken");
        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setStatus(AccountStatus.ACTIVE.name());
        tenant.setSecurityVersion(1);
        when(tenantMapper.selectById(1)).thenReturn(tenant);
        when(userMapper.selectByEmail("taken@example.com")).thenReturn(new User());

        ApiException ex = assertThrows(ApiException.class, () -> userService.register(req));
        assertEquals(ErrorType.BAD_REQUEST, ex.getErrorType());
        verify(emailVerificationService).requireConsumeSuccess("register", "taken@example.com", "123456");
        verify(userMapper, never()).insert(any());
    }
}
