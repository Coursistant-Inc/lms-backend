package com.coursistant.lms.shared.security;

import com.coursistant.lms.shared.security.RequiresPermission;
import com.coursistant.lms.module.permission.service.UserPermissionService;
import com.coursistant.lms.shared.security.TokenUtils;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private UserPermissionService userPermissionService;

    @Before("@annotation(requiresPermission)")
    public void checkPermission(RequiresPermission requiresPermission) {
        String permission = requiresPermission.value();
        Integer userId = TokenUtils.getCurrentUser().getId();  // from JWT/session
        boolean hasPermission = userPermissionService.checkUserPermission(userId, permission);
        if (!hasPermission) {
            throw new CustomException(ResultCodeEnum.USER_NO_PMERMISSION);
        }
    }
}
