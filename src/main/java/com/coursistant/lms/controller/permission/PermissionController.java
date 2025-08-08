package com.coursistant.lms.controller.permission;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Permission;
import com.coursistant.lms.entity.RolePermission;
import com.coursistant.lms.entity.UserPermission;
import com.coursistant.lms.service.permission.PermissionService;
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

    @GetMapping("/selectAll")
    public Result selectAll(Permission permission) {
        logRequest("selectAll", permission != null ? permission.toString() : "null");
        List<Permission> list = permissionService.selectAll(permission);
        logResponse("selectAll", null);
        return Result.success(list);
    }
}