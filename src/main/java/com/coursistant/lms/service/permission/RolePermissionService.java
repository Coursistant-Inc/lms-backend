package com.coursistant.lms.service.permission;

import com.coursistant.lms.entity.Permission;
import com.coursistant.lms.entity.RolePermission;
import com.coursistant.lms.entity.UserPermission;
import com.coursistant.lms.mapper.system.PermissionMapper; // Updated import to the single mapper
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // For transaction management

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

@Service
public class RolePermissionService {

    private static final Logger logger = Logger.getLogger(PermissionService.class.getName());

    @Resource
    private PermissionMapper permissionManagementMapper; // Inject the single mapper

    /**
     * Adds a default permission to a role.
     * Ensures the permission exists before adding.
     * @param rolePermission RolePermission entity containing roleName and permissionId.
     * @return true if added successfully, false otherwise.
     */
    public void addRolePermission(RolePermission rolePermission) { // Changed input to entity
        logger.info("Attempting to add role permission: " + rolePermission.getRoleName() + " - " + rolePermission.getPermissionId());

        RolePermission existing = permissionManagementMapper.selectRolePermission(rolePermission.getRoleName(), rolePermission.getPermissionId());
        if (existing != null) {
            logger.warning("Role permission already exists: " + rolePermission.getRoleName() + " - " + rolePermission.getPermissionId());
            return;
        }

        permissionManagementMapper.insertRolePermission(rolePermission);
    }

    /**
     * Deletes a default permission from a role.
     * Ensures the permission exists before attempting deletion.
     * @param rolePermission RolePermission entity containing roleName and permissionId.
     * @return true if deleted successfully, false otherwise.
     */
    public void deleteRolePermission(RolePermission rolePermission) { // Changed input to entity
        logger.info("Attempting to delete role permission: " + rolePermission.getRoleName() + " - " + rolePermission.getPermissionId());
        permissionManagementMapper.deleteRolePermission(rolePermission);
    }

    public List<RolePermission> selectAll(RolePermission rolePermission){return permissionManagementMapper.selectAllRolePermission(rolePermission);}
}

