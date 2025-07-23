package com.coursistant.lms.service.system;

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
public class PermissionService {

    private static final Logger logger = Logger.getLogger(PermissionService.class.getName());

    @Resource
    private PermissionMapper permissionManagementMapper; // Inject the single mapper

    /**
     * Adds a default permission to a role.
     * Ensures the permission exists before adding.
     * @param rolePermission RolePermission entity containing roleName and permissionId.
     * @return true if added successfully, false otherwise.
     */
    public boolean addRolePermission(RolePermission rolePermission) { // Changed input to entity
        logger.info("Attempting to add role permission: " + rolePermission.getRoleName() + " - " + rolePermission.getPermissionId());

        RolePermission existing = permissionManagementMapper.selectRolePermission(rolePermission.getRoleName(), rolePermission.getPermissionId());
        if (existing != null) {
            logger.warning("Role permission already exists: " + rolePermission.getRoleName() + " - " + rolePermission.getPermissionId());
            return true; // Already exists, consider it successful
        }

        int rowsAffected = permissionManagementMapper.insertRolePermission(rolePermission);
        return rowsAffected > 0;
    }

    /**
     * Deletes a default permission from a role.
     * Ensures the permission exists before attempting deletion.
     * @param rolePermission RolePermission entity containing roleName and permissionId.
     * @return true if deleted successfully, false otherwise.
     */
    public boolean deleteRolePermission(RolePermission rolePermission) { // Changed input to entity
        logger.info("Attempting to delete role permission: " + rolePermission.getRoleName() + " - " + rolePermission.getPermissionId());
        int rowsAffected = permissionManagementMapper.deleteRolePermission(rolePermission);
        return rowsAffected > 0;
    }

    /**
     * Adds or updates a specific permission override for a user.
     * If an override for the user and permission already exists, it updates the 'has_permission' status.
     * Otherwise, it inserts a new override.
     * @param userPermission UserPermission entity containing userId, permissionId, and hasPermission.
     * @return true if added/updated successfully, false otherwise.
     */
    public boolean addUserPermission(UserPermission userPermission) { // Changed input to entity
        logger.info("Attempting to add/update user permission: " + userPermission.getUserId() + " - " + userPermission.getPermissionId() + " - " + userPermission.getHasPermission());
        // No need to get permission by name here, as permissionId is directly available in the entity.

        UserPermission existingOverride = permissionManagementMapper.selectUserPermission(userPermission.getUserId(), userPermission.getPermissionId());
        int rowsAffected;
        if (existingOverride != null) {
                // Update existing override
            rowsAffected = permissionManagementMapper.updateUserPermission(userPermission);
        } else {
            // Insert new override
            rowsAffected = permissionManagementMapper.insertUserPermission(userPermission);
        }
        return rowsAffected > 0;
    }

    /**
     * Deletes a specific permission override for a user.
     * @param userPermission UserPermission entity containing userId and permissionId.
     * @return true if deleted successfully, false otherwise.
     */
    public boolean deleteUserPermission(UserPermission userPermission) { // Changed input to entity
        logger.info("Attempting to delete user permission: " + userPermission.getUserId() + " - " + userPermission.getPermissionId());
        int rowsAffected = permissionManagementMapper.deleteUserPermission(userPermission.getUserId(), userPermission.getPermissionId());
        return rowsAffected > 0;
    }

    /**
     * Checks if a user has a specific effective permission by querying the UserEffectivePermissions view.
     * @param userId The ID of the user.
     * @param permissionName The name of the permission to check.
     * @return true if the user has the permission, false otherwise.
     */
    public boolean checkUserPermission(Integer userId, String permissionName) {
        logger.info("Checking user permission: userId=" + userId + ", permissionName=" + permissionName);

        Boolean hasPermission = permissionManagementMapper.checkUserEffectivePermission(userId, permissionName);
        return Boolean.TRUE.equals(hasPermission);
    }

    /**
     * Retrieves all effective permissions for a given user by querying the UserEffectivePermissions view.
     * @param userId The ID of the user.
     * @return A list of PermissionResponse objects.
     */
    public List<UserPermission> getEffectiveUserPermissions(Integer userId) {
        logger.info("Retrieving effective permissions for userId: " + userId);
        List<UserPermission> permissions = permissionManagementMapper.getEffectivePermissionsByUserId(userId);
        return permissions;
    }
}

