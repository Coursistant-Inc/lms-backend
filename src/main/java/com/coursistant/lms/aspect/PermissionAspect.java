package com.coursistant.lms.aspect;

import com.coursistant.lms.annotation.RequiresPermission;
import com.coursistant.lms.service.permission.UserPermissionService;
import com.coursistant.lms.utils.TokenUtils;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
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
