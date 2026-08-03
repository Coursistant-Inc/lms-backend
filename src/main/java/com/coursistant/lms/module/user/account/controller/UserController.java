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

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.AuthzService;

/**
 * User read API (SYSTEM_ADMIN only). Write endpoints disabled until Phase 2.
 * Profile self-service uses dedicated controllers, not this one.
 */
@RestController
@RequestMapping("/v2/users")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private AuthzService authzService;

    @GetMapping("/{id}")
    public ApiResponse<User> selectById(HttpServletRequest request, @PathVariable Integer id) {
        authzService.requireSystemAdmin(request);
        User user = userService.selectById(id);
        return ApiResponse.success(user);
    }

    @GetMapping
    public ApiResponse<List<User>> selectAll(
            HttpServletRequest request,
            User user,
            @RequestParam(value = "role", required = false) String role) {
        authzService.requireSystemAdmin(request);
        if ("instructor".equalsIgnoreCase(role) || "teacher".equalsIgnoreCase(role)) {
            return ApiResponse.success(userService.selectTeachers());
        }
        return ApiResponse.success(userService.selectAll(user));
    }

    @PostMapping
    public ApiResponse<Void> addDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteBatchDisabled() {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "User write APIs are disabled until secure management APIs ship");
    }

    @PatchMapping("/{id}/password-status")
    public ApiResponse<Void> passwordStatusDisabled(@PathVariable Integer id) {
        throw new ApiException(ErrorType.FORBIDDEN, "password-status is disabled; password flows clear mustChangePassword");
    }
}
