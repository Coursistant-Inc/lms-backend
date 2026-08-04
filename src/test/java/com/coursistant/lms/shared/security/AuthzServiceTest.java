package com.coursistant.lms.shared.security;

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthzServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthzService authzService;

    @Test
    void requireSystemAdmin_allowsSystemAdmin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userRole", "SYSTEM_ADMIN");
        assertDoesNotThrow(() -> authzService.requireSystemAdmin(request));
    }

    @Test
    void requireSystemAdmin_rejectsUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userRole", "USER");
        ApiException ex = assertThrows(ApiException.class, () -> authzService.requireSystemAdmin(request));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
    }

    @Test
    void requireTenantAdminOrSystem_crossTenant_notFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 5);
        request.setAttribute("userRole", "TENANT_ADMIN");
        User user = new User();
        user.setId(5);
        user.setTenantId(1);
        when(userMapper.selectById(5)).thenReturn(user);

        ApiException ex = assertThrows(ApiException.class,
                () -> authzService.requireTenantAdminOrSystem(request, 2));
        assertEquals(ErrorType.NOT_FOUND, ex.getErrorType());
    }

    @Test
    void requireTenantAdminOrSystem_sameTenant_ok() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 5);
        request.setAttribute("userRole", "TENANT_ADMIN");
        User user = new User();
        user.setId(5);
        user.setTenantId(1);
        when(userMapper.selectById(5)).thenReturn(user);

        assertDoesNotThrow(() -> authzService.requireTenantAdminOrSystem(request, 1));
    }

    @Test
    void isSystemAdmin_falseForTenantAdmin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userRole", "TENANT_ADMIN");
        assertFalse(authzService.isSystemAdmin(request));
    }

    @Test
    void isTenantAdmin_trueForTenantAdmin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userRole", "TENANT_ADMIN");
        assertTrue(authzService.isTenantAdmin(request));
    }

    @Test
    void requireUserId_missing_unauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ApiException ex = assertThrows(ApiException.class, () -> authzService.requireUserId(request));
        assertEquals(ErrorType.UNAUTHORIZED, ex.getErrorType());
    }

    @Test
    void systemAdmin_mayAccessAnyTenant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userRole", "SYSTEM_ADMIN");
        assertDoesNotThrow(() -> authzService.requireTenantAdminOrSystem(request, 99));
    }

    @Test
    void requireTenantAdminOrSystem_user_forbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 21);
        request.setAttribute("userRole", "USER");
        ApiException ex = assertThrows(ApiException.class,
                () -> authzService.requireTenantAdminOrSystem(request, 1));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
    }

    @Test
    void requireRole_missing_unauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ApiException ex = assertThrows(ApiException.class, () -> authzService.requireRole(request));
        assertEquals(ErrorType.UNAUTHORIZED, ex.getErrorType());
    }

    @Test
    void resolveActorTenantId_systemAdmin_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userRole", "SYSTEM_ADMIN");
        assertNull(authzService.resolveActorTenantId(request));
    }

    @Test
    void resolveActorTenantId_userWithoutTenant_unauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 21);
        request.setAttribute("userRole", "USER");
        User user = new User();
        user.setId(21);
        user.setTenantId(null);
        when(userMapper.selectById(21)).thenReturn(user);
        ApiException ex = assertThrows(ApiException.class, () -> authzService.resolveActorTenantId(request));
        assertEquals(ErrorType.UNAUTHORIZED, ex.getErrorType());
    }

    @Test
    void requireTenantAdminOrSystem_nullResourceTenant_notFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 5);
        request.setAttribute("userRole", "TENANT_ADMIN");
        ApiException ex = assertThrows(ApiException.class,
                () -> authzService.requireTenantAdminOrSystem(request, null));
        assertEquals(ErrorType.NOT_FOUND, ex.getErrorType());
    }
}
