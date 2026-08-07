package com.coursistant.lms.module.auth.admin.controller;

import com.coursistant.lms.module.auth.admin.dto.AdminQuery;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.AuthzService;
import com.coursistant.lms.shared.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerAuthzTest {

    @Mock
    private AdminService adminService;
    @Mock
    private AuthzService authzService;

    @InjectMocks
    private AdminController adminController;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
    }

    @Test
    void selectAll_requiresSystemAdmin() {
        doThrow(new ApiException(ErrorType.FORBIDDEN)).when(authzService).requireSystemAdmin(request);
        ApiException ex = assertThrows(ApiException.class, () -> adminController.selectAll(request, new AdminQuery()));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
        verifyNoInteractions(adminService);
    }

    @Test
    void selectAll_systemAdmin_ok() {
        doNothing().when(authzService).requireSystemAdmin(request);
        when(adminService.selectAll(any())).thenReturn(List.of(new Admin()));
        assertEquals(1, adminController.selectAll(request, new AdminQuery()).getData().size());
    }

    @Test
    void writeEndpoints_disabled() {
        ApiException ex = assertThrows(ApiException.class, () -> adminController.addDisabled());
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
    }
}
