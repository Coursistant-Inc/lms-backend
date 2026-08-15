package com.coursistant.lms.module.auth.admin.controller;

import com.coursistant.lms.module.auth.admin.dto.AdminQuery;
import com.coursistant.lms.module.auth.admin.dto.AdminResponse;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.service.AdminService;
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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * System-admin read API. Write endpoints are disabled until Phase 2 secure management APIs.
 */
@RestController
@RequestMapping("/v2/admins")
@Tag(name = "Admins", description = "System admin read APIs; write APIs disabled until Phase 2")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private AuthzService authzService;

    @GetMapping("/{id}")
    @Operation(operationId = "adminGetById", summary = "Get admin by id")
    public ApiResponse<AdminResponse> selectById(HttpServletRequest request, @PathVariable Integer id) {
        authzService.requireSystemAdmin(request);
        return ApiResponse.success(toResponse(adminService.selectById(id)));
    }

    @GetMapping
    @Operation(operationId = "adminList", summary = "List admins with optional filters")
    public ApiResponse<List<AdminResponse>> selectAll(HttpServletRequest request, AdminQuery query) {
        authzService.requireSystemAdmin(request);
        List<AdminResponse> list = adminService.selectAll(toProbe(query)).stream()
                .map(AdminController::toResponse)
                .toList();
        return ApiResponse.success(list);
    }

    @org.springframework.web.bind.annotation.PostMapping
    @Operation(
            operationId = "adminAddDisabled",
            summary = "Create admin (disabled)",
            description = "Phase 2 disabled. Always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            }))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> addDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @Operation(
            operationId = "adminDeleteDisabled",
            summary = "Delete admin (disabled)",
            description = "Phase 2 disabled. Always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            }))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> deleteDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/batch")
    @Operation(
            operationId = "adminDeleteBatchDisabled",
            summary = "Batch delete admins (disabled)",
            description = "Phase 2 disabled. Always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            }))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> deleteBatchDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @Operation(
            operationId = "adminUpdateDisabled",
            summary = "Update admin (disabled)",
            description = "Phase 2 disabled. Always returns 403 FORBIDDEN until secure management APIs ship.",
            deprecated = true,
            extensions = @Extension(properties = {
                    @ExtensionProperty(name = "x-availability", value = "disabled")
            }))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN — write API disabled",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> updateDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }

    private static Admin toProbe(AdminQuery query) {
        if (query == null) {
            return null;
        }
        Admin admin = new Admin();
        admin.setId(query.getId());
        admin.setUsername(query.getUsername());
        admin.setName(query.getName());
        admin.setPhone(query.getPhone());
        admin.setEmail(query.getEmail());
        admin.setAvatar(query.getAvatar());
        admin.setRole(query.getRole());
        admin.setStatus(query.getStatus());
        return admin;
    }

    public static AdminResponse toResponse(Admin admin) {
        if (admin == null) {
            return null;
        }
        AdminResponse response = new AdminResponse();
        response.setId(admin.getId());
        response.setUsername(admin.getUsername());
        response.setName(admin.getName());
        response.setPhone(admin.getPhone());
        response.setEmail(admin.getEmail());
        response.setAvatar(admin.getAvatar());
        response.setRole(admin.getRole());
        response.setStatus(admin.getStatus());
        return response;
    }
}
