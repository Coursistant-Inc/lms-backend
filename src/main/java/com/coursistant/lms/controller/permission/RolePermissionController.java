package com.coursistant.lms.controller.permission;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Permission;
import com.coursistant.lms.entity.RolePermission;
import com.coursistant.lms.entity.UserPermission;
import com.coursistant.lms.service.permission.RolePermissionService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/rolePermission")
public class RolePermissionController {

    @Resource
    private RolePermissionService rolePermissionService;

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
    public Result addRolePermission(@RequestBody RolePermission rolePermission) {
        logRequest("addRolePermission", rolePermission.toString());
        rolePermissionService.addRolePermission(rolePermission);
        logResponse("addRolePermission", rolePermission.toString());
        return Result.success();
    }

    /**
     * Deletes a default permission from a role.
     * DELETE /permission/role/delete
     * Request Body: { "roleName": "TEACHER", "permissionName": "course_delete" }
     */
    @DeleteMapping("/delete")
    public Result deleteRolePermission(@RequestBody RolePermission rolePermission) {
        logRequest("deleteRolePermission", rolePermission.toString());
        rolePermissionService.deleteRolePermission(rolePermission);
        logResponse("deleteRolePermission", rolePermission.toString());
        return Result.success();
    }

    @GetMapping("/selectAll")
    public Result selectAll(RolePermission rolePermission) {
        logRequest("selectAll", rolePermission != null ? rolePermission.toString() : "null");
        List<RolePermission> list = rolePermissionService.selectAll(rolePermission);
        logResponse("selectAll", null);
        return Result.success(list);
    }
}
