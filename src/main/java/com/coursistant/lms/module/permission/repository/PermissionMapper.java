package com.coursistant.lms.module.permission.repository;

import com.coursistant.lms.module.permission.entity.Permission;
import com.coursistant.lms.module.permission.entity.RolePermission;
import com.coursistant.lms.module.permission.entity.UserPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PermissionMapper {

    // --- Methods for 'Permissions' table ---

    int insertPermission(Permission permission);

    List<Permission> selectAllPermission(Permission permission);

    // --- Methods for 'RolePermissions' table ---
    int insertRolePermission(RolePermission rolePermission);

    int deleteRolePermission(RolePermission rolePermission);

    RolePermission selectRolePermission(String roleName, Integer permissionId);

    List<RolePermission> selectAllRolePermission(RolePermission rolePermission);

    // --- Methods for 'UserPermissions' table ---
    int insertUserPermission(UserPermission userPermission);

    int updateUserPermission(UserPermission userPermission);

    int deleteUserPermission(Integer userId, Integer permissionId);

    UserPermission selectUserPermission(Integer userId, Integer permissionId);

    List<UserPermission> selectAllUserPermission(UserPermission userPermission);
    // --- Methods for 'UserEffectivePermissions' view ---

    Boolean checkUserEffectivePermission(Integer userId, String permissionName);

    List<Permission> getEffectivePermissionsByUserId(Integer userId);

}