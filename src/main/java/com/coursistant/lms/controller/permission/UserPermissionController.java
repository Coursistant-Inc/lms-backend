package com.coursistant.lms.controller.permission;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Permission;
import com.coursistant.lms.entity.RolePermission;
import com.coursistant.lms.entity.UserPermission;
import com.coursistant.lms.service.permission.UserPermissionService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/userPermission")
public class UserPermissionController {

    @Resource
    private UserPermissionService userPermissionService;

    private static final Logger logger = Logger.getLogger(PermissionController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * Adds or updates a specific permission override for a user.
     * POST /permission/user/add
     * Request Body: { "userId": 101, "permissionName": "course_delete", "hasPermission": false }
     */
    @PostMapping("/add")
    public Result addUserPermission(@RequestBody UserPermission userPermission) {
        logRequest("addUserPermission", userPermission.toString());
        userPermissionService.addUserPermission(userPermission);
        logResponse("addUserPermission", userPermission.toString());
        return Result.success();
    }

    /**
     * Deletes a specific permission override for a user.
     * DELETE /permission/user/delete
     * Request Body: { "userId": 101, "permissionName": "course_delete" }
     */
    @DeleteMapping("/delete")
    public Result deleteUserPermission(@RequestBody UserPermission userPermission) {
        logRequest("deleteUserPermission", userPermission.toString());
        userPermissionService.deleteUserPermission(userPermission);
        logResponse("deleteUserPermission", userPermission.toString());
        return Result.success();
    }

    /**
     * Checks if a user has a specific effective permission.
     * GET /permission/user/{userId}/check/{permissionName}
     */
    @GetMapping("/check/{userId}/{permissionName}")
    public Result checkUserPermission(@PathVariable Integer userId, @PathVariable String permissionName) {
        logRequest("checkUserPermission", String.format("userId=%d, permissionName=%s", userId, permissionName));
        boolean hasPermission = userPermissionService.checkUserPermission(userId, permissionName);
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
        List<Permission> permissions = userPermissionService.getEffectiveUserPermissions(userId);
        logResponse("getEffectiveUserPermissions", permissions.toString());
        return Result.success(permissions);
    }

    @GetMapping("/selectAll")
    public Result selectAll(UserPermission userPermission) {
        logRequest("selectAll", userPermission != null ? userPermission.toString() : "null");
        List<UserPermission> list = userPermissionService.selectAll(userPermission);
        logResponse("selectAll", null);
        return Result.success(list);
    }
}
