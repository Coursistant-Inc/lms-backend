package com.coursistant.lms.shared.security;

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Platform / tenant authorization helpers. Does not grant academic write
 * (submit/attempt/grade); callers must keep teaching/student checks separate.
 */
@Service
public class AuthzService {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USER_ROLE = "userRole";

    @Resource
    private UserMapper userMapper;

    public Integer requireUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(ATTR_USER_ID);
        if (!(userId instanceof Integer)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return (Integer) userId;
    }

    public String requireRole(HttpServletRequest request) {
        Object role = request.getAttribute(ATTR_USER_ROLE);
        if (!(role instanceof String) || ((String) role).isBlank()) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return (String) role;
    }

    /** Platform SYSTEM_ADMIN - cross-tenant administrative access. */
    public boolean isSystemAdmin(HttpServletRequest request) {
        return RoleEnum.SYSTEM_ADMIN.name().equals(request.getAttribute(ATTR_USER_ROLE));
    }

    public void requireSystemAdmin(HttpServletRequest request) {
        if (!isSystemAdmin(request)) {
            throw new ApiException(ErrorType.FORBIDDEN, "System admin required");
        }
    }

    public boolean isTenantAdmin(HttpServletRequest request) {
        return RoleEnum.TENANT_ADMIN.name().equals(request.getAttribute(ATTR_USER_ROLE));
    }

    /**
     * Actor tenant from DB for USER / TENANT_ADMIN. SYSTEM_ADMIN has no tenant (null).
     */
    public Integer resolveActorTenantId(HttpServletRequest request) {
        if (isSystemAdmin(request)) {
            return null;
        }
        Integer userId = requireUserId(request);
        User user = userMapper.selectById(userId);
        if (user == null || user.getTenantId() == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return user.getTenantId();
    }

    /**
     * SYSTEM_ADMIN allowed; TENANT_ADMIN only when actor tenant matches resource tenant
     * (cross-tenant returns 404); USER returns 403.
     */
    public void requireTenantAdminOrSystem(HttpServletRequest request, Integer resourceTenantId) {
        if (isSystemAdmin(request)) {
            return;
        }
        if (!isTenantAdmin(request)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        if (resourceTenantId == null) {
            throw new ApiException(ErrorType.NOT_FOUND);
        }
        Integer actorTenantId = resolveActorTenantId(request);
        if (!resourceTenantId.equals(actorTenantId)) {
            throw new ApiException(ErrorType.NOT_FOUND);
        }
    }
}
