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
}
