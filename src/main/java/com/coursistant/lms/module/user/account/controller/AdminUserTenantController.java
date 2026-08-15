package com.coursistant.lms.module.user.account.controller;

import com.coursistant.lms.module.user.account.dto.PatchUserTenantRequest;
import com.coursistant.lms.module.user.account.dto.UserAdminResponse;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/admin/users")
@Tag(name = "Admin Users", description = "Platform-admin user tenant migration")
public class AdminUserTenantController {

    @Resource
    private UserService userService;

    @Resource
    private AuthzService authzService;

    @Idempotent
    @PatchMapping("/{id}/tenant")
    @Operation(operationId = "adminUserChangeTenant", summary = "Change a user's tenant")
    public ApiResponse<UserAdminResponse> changeTenant(HttpServletRequest request,
                                                       @PathVariable Integer id,
                                                       @RequestBody PatchUserTenantRequest body) {
        // Platform administrative tenant migration
        authzService.requireSystemAdmin(request);
        if (body == null || body.getTenantId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "tenantId is required");
        }
        return ApiResponse.success(UserController.toResponse(userService.changeTenant(id, body.getTenantId())));
    }
}
