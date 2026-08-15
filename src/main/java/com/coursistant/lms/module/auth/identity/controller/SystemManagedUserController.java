package com.coursistant.lms.module.auth.identity.controller;

import com.coursistant.lms.module.auth.identity.dto.SystemManagedUserChangeRoleRequest;
import com.coursistant.lms.module.auth.identity.dto.SystemManagedUserCreateRequest;
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
@RequestMapping("/v2/system/managed-users")
@Tag(name = "System Managed Users", description = "System-admin managed user lifecycle")
public class SystemManagedUserController {

    @Resource
    private ManagedUserService managedUserService;
    @Resource
    private AuthzService authzService;

    @Idempotent
    @PostMapping
    @Operation(operationId = "systemManagedUserCreate", summary = "Create a managed user in a tenant")
    public ApiResponse<Integer> create(
            HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SystemManagedUserCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "createTenantAdmin",
                                    value = "{\"email\":\"admin@example.com\",\"name\":\"Tenant Admin\",\"role\":\"TENANT_ADMIN\",\"level\":\"NOT_APPLICABLE\",\"tenantId\":1}")))
            @RequestBody SystemManagedUserCreateRequest body) {
        authzService.requireSystemAdmin(request);
        if (body == null || body.email == null || body.name == null || body.role == null || body.tenantId == null) {
            throw new ApiException(ErrorType.PARAM_MISSING);
        }
        Integer id = managedUserService.createUser(request,
                new ManagedUserService.CreateManagedUserCommand(body.email, body.name, body.role, body.level, body.tenantId),
                true);
        return ApiResponse.success(id);
    }

    @Idempotent
    @PutMapping("/{id}/role")
    @Operation(operationId = "systemManagedUserChangeRole", summary = "Change role/level of a system-managed user")
    public ApiResponse<Void> changeRole(
            HttpServletRequest request,
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SystemManagedUserChangeRoleRequest.class),
                            examples = @ExampleObject(
                                    name = "changeRole",
                                    value = "{\"role\":\"USER\",\"level\":\"STUDENT\"}")))
            @RequestBody SystemManagedUserChangeRoleRequest body) {
        authzService.requireSystemAdmin(request);
        if (body == null || body.role == null) {
            throw new ApiException(ErrorType.PARAM_MISSING);
        }
        managedUserService.changeRole(request, id, body.role, body.level);
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/{id}/disable")
    @Operation(operationId = "systemManagedUserDisable", summary = "Disable a system-managed user")
    public ApiResponse<Void> disable(HttpServletRequest request, @PathVariable Integer id) {
        authzService.requireSystemAdmin(request);
        managedUserService.disableUser(request, id);
        return ApiResponse.success();
    }
}
