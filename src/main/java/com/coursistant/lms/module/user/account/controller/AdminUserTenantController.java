package com.coursistant.lms.module.user.account.controller;

import com.coursistant.lms.module.user.account.dto.PatchUserTenantRequest;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/admin/users")
public class AdminUserTenantController {

    @Resource
    private UserService userService;

    @Resource
    private AuthzService authzService;

    @Idempotent
    @PatchMapping("/{id}/tenant")
    public ApiResponse<User> changeTenant(HttpServletRequest request,
                                          @PathVariable Integer id,
                                          @RequestBody PatchUserTenantRequest body) {
        // Platform administrative tenant migration
        authzService.requireSystemAdmin(request);
        if (body == null || body.getTenantId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "tenantId is required");
        }
        return ApiResponse.success(userService.changeTenant(id, body.getTenantId()));
    }
}
