package com.coursistant.lms.module.auth.identity.controller;

import com.coursistant.lms.module.auth.identity.dto.TenantManagedUserChangeRoleRequest;
import com.coursistant.lms.module.auth.identity.dto.TenantManagedUserCreateRequest;
import com.coursistant.lms.module.auth.identity.service.ManagedUserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/tenant/managed-users")
@Tag(name = "Tenant Managed Users", description = "Tenant-admin managed user lifecycle")
public class TenantManagedUserController {

    @Resource
    private ManagedUserService managedUserService;
    @Resource
    private AuthzService authzService;

    @Idempotent
    @PostMapping
    @Operation(operationId = "tenantManagedUserCreate", summary = "Create a managed user in the caller tenant")
    public ApiResponse<Integer> create(
            HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TenantManagedUserCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "createTa",
                                    value = "{\"email\":\"ta@example.com\",\"name\":\"TA One\",\"role\":\"USER\",\"level\":\"TA\"}")))
            @RequestBody TenantManagedUserCreateRequest body) {
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
    @Operation(operationId = "tenantManagedUserChangeRole", summary = "Change role/level of a tenant-managed user")
    public ApiResponse<Void> changeRole(
            HttpServletRequest request,
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TenantManagedUserChangeRoleRequest.class),
                            examples = @ExampleObject(
                                    name = "changeRole",
                                    value = "{\"role\":\"USER\",\"level\":\"STUDENT\"}")))
            @RequestBody TenantManagedUserChangeRoleRequest body) {
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
    @Operation(operationId = "tenantManagedUserDisable", summary = "Disable a tenant-managed user")
    public ApiResponse<Void> disable(HttpServletRequest request, @PathVariable Integer id) {
        if (!authzService.isTenantAdmin(request)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        managedUserService.disableUser(request, id);
        return ApiResponse.success();
    }
}
