package com.coursistant.lms.controller.system;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Permission;
import com.coursistant.lms.entity.RolePermission;
import com.coursistant.lms.entity.UserPermission;
import com.coursistant.lms.service.system.PermissionService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

/**
 * REST Controller for managing permissions (RolePermissions and UserPermissions).
 * It also provides endpoints to check and retrieve effective user permissions.
 */
@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    private static final Logger logger = Logger.getLogger(PermissionController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * Adds a default permission to a role.
     */
    @PostMapping("/add")
    public Result addPermission(@RequestBody Permission permission) {
        logRequest("addPermission", permission.toString());
        permissionService.addPermission(permission);
        logResponse("addPermission", permission.toString());
        return Result.success();
    }

    /**
     * Adds a default permission to a role.
     */
    @PostMapping("/role/add")
    public Result addRolePermission(@RequestBody RolePermission rolePermission) {
        logRequest("addRolePermission", rolePermission.toString());
        permissionService.addRolePermission(rolePermission);
        logResponse("addRolePermission", rolePermission.toString());
        return Result.success();
    }

    /**
     * Deletes a default permission from a role.
     * DELETE /permission/role/delete
     * Request Body: { "roleName": "TEACHER", "permissionName": "course_delete" }
     */
    @DeleteMapping("/role/delete")
    public Result deleteRolePermission(@RequestBody RolePermission rolePermission) {
        logRequest("deleteRolePermission", rolePermission.toString());
        permissionService.deleteRolePermission(rolePermission);
        logResponse("deleteRolePermission", rolePermission.toString());
        return Result.success();
    }

    /**
     * Adds or updates a specific permission override for a user.
     * POST /permission/user/add
     * Request Body: { "userId": 101, "permissionName": "course_delete", "hasPermission": false }
     */
    @PostMapping("/user/add")
    public Result addUserPermission(@RequestBody UserPermission userPermission) {
        logRequest("addUserPermission", userPermission.toString());
        permissionService.addUserPermission(userPermission);
        logResponse("addUserPermission", userPermission.toString());
        return Result.success();
    }

    /**
     * Deletes a specific permission override for a user.
     * DELETE /permission/user/delete
     * Request Body: { "userId": 101, "permissionName": "course_delete" }
     */
    @DeleteMapping("/user/delete")
    public Result deleteUserPermission(@RequestBody UserPermission userPermission) {
        logRequest("deleteUserPermission", userPermission.toString());
        permissionService.deleteUserPermission(userPermission);
        logResponse("deleteUserPermission", userPermission.toString());
        return Result.success();
    }

    /**
     * Checks if a user has a specific effective permission.
     * GET /permission/user/{userId}/check/{permissionName}
     */
    @GetMapping("/user/{userId}/check/{permissionName}")
    public Result checkUserPermission(@PathVariable Integer userId, @PathVariable String permissionName) {
        logRequest("checkUserPermission", String.format("userId=%d, permissionName=%s", userId, permissionName));
        boolean hasPermission = permissionService.checkUserPermission(userId, permissionName);
        logResponse("checkUserPermission", String.valueOf(hasPermission));
        return Result.success(hasPermission);
    }

    /**
     * Retrieves all effective permissions for a given user.
     * GET /permission/user/{userId}/effectivePermissions
     */
    @GetMapping("/effectivePermissions/{userId}")
    public Result getEffectiveUserPermissions(@PathVariable Integer userId) {
        logRequest("getEffectiveUserPermissions", String.valueOf(userId));
        List<Permission> permissions = permissionService.getEffectiveUserPermissions(userId);
        logResponse("getEffectiveUserPermissions", permissions.toString());
        return Result.success(permissions);
    }
}