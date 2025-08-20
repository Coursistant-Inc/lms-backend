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
public class UserPermissionService {

    private static final Logger logger = Logger.getLogger(PermissionService.class.getName());

    @Resource
    private PermissionMapper permissionManagementMapper; // Inject the single mapper

    public void addUserPermission(UserPermission userPermission) { // Changed input to entity
        logger.info("Attempting to add/update user permission: " + userPermission.getUserId() + " - " + userPermission.getPermissionId() + " - " + userPermission.getHasPermission());
        // No need to get permission by name here, as permissionId is directly available in the entity.

        UserPermission existingOverride = permissionManagementMapper.selectUserPermission(userPermission.getUserId(), userPermission.getPermissionId());
        int rowsAffected;
        if (existingOverride != null) {
            // Update existing override
            permissionManagementMapper.updateUserPermission(userPermission);
        } else {
            // Insert new override
            permissionManagementMapper.insertUserPermission(userPermission);
        }
    }

    /**
     * Deletes a specific permission override for a user.
     * @param userPermission UserPermission entity containing userId and permissionId.
     * @return true if deleted successfully, false otherwise.
     */
    public void deleteUserPermission(UserPermission userPermission) { // Changed input to entity
        logger.info("Attempting to delete user permission: " + userPermission.getUserId() + " - " + userPermission.getPermissionId());
        permissionManagementMapper.deleteUserPermission(userPermission.getUserId(), userPermission.getPermissionId());
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
    public List<Permission> getEffectiveUserPermissions(Integer userId) {
        logger.info("Retrieving effective permissions for userId: " + userId);
        List<Permission> permissions = permissionManagementMapper.getEffectivePermissionsByUserId(userId);
        return permissions;
    }

    public List<UserPermission> selectAll(UserPermission userPermission){return permissionManagementMapper.selectAllUserPermission(userPermission); }
}

