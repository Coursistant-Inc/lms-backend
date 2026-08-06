package com.coursistant.lms.shared.security;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.RoleEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Resolves {@link ActorContext} from verified request attributes + DB.
 * Disabled principal / tenant → {@link ErrorType#UNAUTHORIZED} (same family as invalid session).
 */
@Service
public class ActorContextResolver {

    @Resource
    private UserMapper userMapper;
    @Resource
    private AdminMapper adminMapper;
    @Resource
    private TenantMapper tenantMapper;

    public ActorContext resolve(HttpServletRequest request) {
        Integer actorId = requireAttrUserId(request);
        String role = requireAttrRole(request);

        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            Admin admin = adminMapper.selectById(actorId);
            if (admin == null || !isActive(admin.getStatus())) {
                throw new ApiException(ErrorType.UNAUTHORIZED);
            }
            return new ActorContext(ActorContext.ACTOR_ADMIN, actorId, role, null, null, admin.getStatus());
        }

        User user = userMapper.selectById(actorId);
        if (user == null || !isActive(user.getStatus())) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        if (user.getTenantId() == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        Tenant tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null || !isActive(tenant.getStatus())) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        if (RoleEnum.TENANT_ADMIN.name().equals(role)) {
            return new ActorContext(ActorContext.ACTOR_USER, actorId, role,
                    user.getTenantId(), user.getLevel(), user.getStatus());
        }
        if (RoleEnum.USER.name().equals(role)) {
            if (user.getLevel() == null || user.getLevel().isBlank()) {
                throw new ApiException(ErrorType.UNAUTHORIZED);
            }
            return new ActorContext(ActorContext.ACTOR_USER, actorId, role,
                    user.getTenantId(), user.getLevel(), user.getStatus());
        }
        throw new ApiException(ErrorType.UNAUTHORIZED);
    }

    private static Integer requireAttrUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(AuthzService.ATTR_USER_ID);
        if (!(userId instanceof Integer)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return (Integer) userId;
    }

    private static String requireAttrRole(HttpServletRequest request) {
        Object role = request.getAttribute(AuthzService.ATTR_USER_ROLE);
        if (!(role instanceof String) || ((String) role).isBlank()) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return (String) role;
    }

    private static boolean isActive(String status) {
        return status == null || AccountStatus.ACTIVE.name().equals(status);
    }
}
