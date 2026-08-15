package com.coursistant.lms.module.user.account.controller;

import java.util.List;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coursistant.lms.module.user.account.dto.UserAdminResponse;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.AuthzService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * User read API (SYSTEM_ADMIN only). Write endpoints disabled until Phase 2.
 * Profile self-service uses dedicated controllers, not this one.
 */
@RestController
@RequestMapping("/v2/users")
@Tag(name = "Users", description = "System-admin user account reads; write APIs disabled")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private AuthzService authzService;

    @GetMapping("/{id}")
    @Operation(operationId = "userGetById", summary = "Get user by ID")
    public ApiResponse<UserAdminResponse> selectById(HttpServletRequest request, @PathVariable Integer id) {
        authzService.requireSystemAdmin(request);
        User user = userService.selectById(id);
        return ApiResponse.success(toResponse(user));
    }

    @GetMapping
    @Operation(operationId = "userList", summary = "List users")
    public ApiResponse<List<UserAdminResponse>> selectAll(
            HttpServletRequest request,
            User user,
            @RequestParam(value = "role", required = false) String role) {
        authzService.requireSystemAdmin(request);
        List<User> users;
        if ("instructor".equalsIgnoreCase(role) || "teacher".equalsIgnoreCase(role)) {
            users = userService.selectTeachers();
        } else {
            users = userService.selectAll(user);
        }
        return ApiResponse.success(users.stream().map(UserController::toResponse).toList());
    }

    @PostMapping
    @Operation(
            operationId = "userAddDisabled",
            summary = "Create user (disabled)",
            description = "Phase 2 disabled — always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            })
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> addDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @DeleteMapping("/{id}")
    @Operation(
            operationId = "userDeleteDisabled",
            summary = "Delete user (disabled)",
            description = "Phase 2 disabled — always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            })
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> deleteDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @DeleteMapping("/batch")
    @Operation(
            operationId = "userDeleteBatchDisabled",
            summary = "Batch delete users (disabled)",
            description = "Phase 2 disabled — always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            })
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> deleteBatchDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @PutMapping("/{id}")
    @Operation(
            operationId = "userUpdateDisabled",
            summary = "Update user (disabled)",
            description = "Phase 2 disabled — always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            })
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> updateDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @PatchMapping("/{id}/password-status")
    @Operation(
            operationId = "userPasswordStatusDisabled",
            summary = "Update password-status (disabled)",
            description = "Phase 2 disabled — always returns 403 FORBIDDEN; password flows clear mustChangePassword.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            })
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> passwordStatusDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "password-status is disabled; password flows clear mustChangePassword");
    }

    public static UserAdminResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        UserAdminResponse response = new UserAdminResponse();
        response.setId(user.getId());
        response.setTenantId(user.getTenantId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setLevel(user.getLevel());
        response.setStatus(user.getStatus());
        response.setAvatar(user.getAvatar());
        response.setMustChangePassword(user.getMustChangePassword());
        response.setEmailNotifications(user.getEmailNotifications());
        return response;
    }
}
