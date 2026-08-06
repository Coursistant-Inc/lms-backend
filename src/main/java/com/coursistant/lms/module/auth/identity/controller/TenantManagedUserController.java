package com.coursistant.lms.module.auth.identity.controller;

import com.coursistant.lms.module.auth.identity.service.ManagedUserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/tenant/managed-users")
public class TenantManagedUserController {

    public static class CreateRequest {
        public String email;
        public String name;
        public String role;
        public String level;
    }

    public static class ChangeRoleRequest {
        public String role;
        public String level;
    }

    @Resource
    private ManagedUserService managedUserService;
    @Resource
    private AuthzService authzService;

    @Idempotent
    @PostMapping
    public ApiResponse<Integer> create(HttpServletRequest request, @RequestBody CreateRequest body) {
        if (!authzService.isTenantAdmin(request)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        if (body == null || body.email == null || body.name == null || body.role == null) {
            throw new ApiException(ErrorType.PARAM_MISSING);
        }
        Integer id = managedUserService.createUser(request,
                new ManagedUserService.CreateManagedUserCommand(body.email, body.name, body.role, body.level, null),
                false);
        return ApiResponse.success(id);
    }

    @Idempotent
    @PutMapping("/{id}/role")
    public ApiResponse<Void> changeRole(HttpServletRequest request, @PathVariable Integer id,
                                        @RequestBody ChangeRoleRequest body) {
        if (!authzService.isTenantAdmin(request)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        if (body == null || body.role == null) {
            throw new ApiException(ErrorType.PARAM_MISSING);
        }
        managedUserService.changeRole(request, id, body.role, body.level);
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(HttpServletRequest request, @PathVariable Integer id) {
        if (!authzService.isTenantAdmin(request)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        managedUserService.disableUser(request, id);
        return ApiResponse.success();
    }
}
