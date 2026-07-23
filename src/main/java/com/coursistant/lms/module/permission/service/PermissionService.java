package com.coursistant.lms.module.permission.service;

import com.coursistant.lms.module.permission.entity.Permission;
import com.coursistant.lms.module.permission.entity.RolePermission;
import com.coursistant.lms.module.permission.entity.UserPermission;
import com.coursistant.lms.module.permission.repository.PermissionMapper; // Updated import to the single mapper
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


    public void addPermission(Permission permission) { // Changed input to entity
        permissionManagementMapper.insertPermission(permission);
    }

    public List<Permission> selectAll(Permission permission){return permissionManagementMapper.selectAllPermission(permission); }
}

