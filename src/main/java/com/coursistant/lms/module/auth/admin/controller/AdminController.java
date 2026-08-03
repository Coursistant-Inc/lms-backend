package com.coursistant.lms.module.auth.admin.controller;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.service.AdminService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.AuthzService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private AuthzService authzService;

    @GetMapping("/{id}")
    public ApiResponse<Admin> selectById(HttpServletRequest request, @PathVariable Integer id) {
        authzService.requireSystemAdmin(request);
        Admin admin = adminService.selectById(id);
        return ApiResponse.success(admin);
    }

    @GetMapping
    public ApiResponse<List<Admin>> selectAll(HttpServletRequest request, Admin admin) {
        authzService.requireSystemAdmin(request);
        List<Admin> list = adminService.selectAll(admin);
        return ApiResponse.success(list);
    }

    @org.springframework.web.bind.annotation.PostMapping
    public ApiResponse<Void> addDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/batch")
    public ApiResponse<Void> deleteBatchDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ApiResponse<Void> updateDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "Admin write APIs are disabled until secure management APIs ship");
    }
}
