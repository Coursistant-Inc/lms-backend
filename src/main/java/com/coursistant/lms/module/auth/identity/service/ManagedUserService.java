package com.coursistant.lms.module.auth.identity.service;

import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.AuthzService;
import com.coursistant.lms.shared.security.SessionInvalidationService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ManagedUserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;
    @Resource
    private AccountIdentityService accountIdentityService;
    @Resource
    private IdentityAuditService identityAuditService;
    @Resource
    private SessionInvalidationService sessionInvalidationService;
    @Resource
    private AuthzService authzService;

    public record CreateManagedUserCommand(String email, String name, String role, String level, Integer tenantId) {}

    @Transactional
    public Integer createUser(HttpServletRequest request, CreateManagedUserCommand cmd, boolean systemScope) {
        Integer actorId = authzService.requireUserId(request);
        String actorRole = authzService.requireRole(request);
        Integer actorTenantId = authzService.resolveActorTenantId(request);

        if (systemScope) {
            authzService.requireSystemAdmin(request);
        } else {
            if (!authzService.isTenantAdmin(request)) {
                throw new ApiException(ErrorType.FORBIDDEN);
            }
        }

        String role = cmd.role();
        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            throw new ApiException(ErrorType.FORBIDDEN, "Cannot create SYSTEM_ADMIN via user management");
        }
        if (!RoleEnum.USER.name().equals(role) && !RoleEnum.TENANT_ADMIN.name().equals(role)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Invalid role");
        }

        Integer tenantId = systemScope ? cmd.tenantId() : actorTenantId;
        if (tenantId == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "tenantId is required");
        }
        if (!systemScope && !tenantId.equals(actorTenantId)) {
            throw new ApiException(ErrorType.NOT_FOUND);
        }

        String level = RoleEnum.TENANT_ADMIN.name().equals(role)
                ? LevelEnum.NOT_APPLICABLE.level
                : (cmd.level() == null ? LevelEnum.STUDENT.level : cmd.level());

        String tempPassword = generateTempPassword();
        User user = new User();
        user.setEmail(AccountIdentityService.normalizeEmail(cmd.email()));
        user.setName(cmd.name());
        user.setUsername(user.getEmail().split("@")[0]);
        user.setRole(role);
        user.setLevel(level);
        user.setTenantId(tenantId);
        user.setEncryptPassword(tempPassword);
        user.setMustChangePassword(true);
        userMapper.insert(user);

        accountIdentityService.claimEmail(user.getEmail(), AccountIdentityService.PRINCIPAL_USER, user.getId());
        identityAuditService.writeSuccess(actorId, actorRole, actorTenantId, "CREATE_USER", "USER",
                user.getId(), tenantId, null, "{\"role\":\"" + role + "\",\"level\":\"" + level + "\"}",
                null, request.getRemoteAddr());

        // Temporary password is not returned; email outbox would pick it up in a follow-up job.
        queueTempPasswordEmail(user.getEmail(), tempPassword);
        return user.getId();
    }

    @Transactional
    public void changeRole(HttpServletRequest request, Integer targetUserId, String newRole, String newLevelIfUser) {
        Integer actorId = authzService.requireUserId(request);
        String actorRole = authzService.requireRole(request);
        User target = userService.selectById(targetUserId);
        if (target == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (authzService.isSystemAdmin(request)) {
            // ok
        } else if (authzService.isTenantAdmin(request)) {
            authzService.requireTenantAdminOrSystem(request, target.getTenantId());
            if (actorId.equals(targetUserId)) {
                throw new ApiException(ErrorType.FORBIDDEN, "Cannot modify self");
            }
        } else {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        if (RoleEnum.SYSTEM_ADMIN.name().equals(newRole)) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        if (RoleEnum.TENANT_ADMIN.name().equals(target.getRole())
                && RoleEnum.USER.name().equals(newRole)) {
            ensureNotLastTenantAdmin(target.getTenantId(), targetUserId);
        }

        String before = "{\"role\":\"" + target.getRole() + "\",\"level\":\"" + target.getLevel() + "\"}";
        target.setRole(newRole);
        if (RoleEnum.TENANT_ADMIN.name().equals(newRole)) {
            target.setLevel(LevelEnum.NOT_APPLICABLE.level);
        } else {
            if (newLevelIfUser == null
                    || (!LevelEnum.INSTRUCTOR.level.equals(newLevelIfUser)
                    && !LevelEnum.STUDENT.level.equals(newLevelIfUser))) {
                throw new ApiException(ErrorType.BAD_REQUEST, "USER requires INSTRUCTOR or STUDENT level");
            }
            target.setLevel(newLevelIfUser);
        }
        userMapper.updateById(target);
        sessionInvalidationService.invalidatePrincipal(targetUserId, beforeRoleOr(target, newRole));
        // Invalidate using previous role family (user table roles)
        sessionInvalidationService.invalidatePrincipal(targetUserId, RoleEnum.USER.name());
        sessionInvalidationService.invalidatePrincipal(targetUserId, RoleEnum.TENANT_ADMIN.name());

        identityAuditService.writeSuccess(actorId, actorRole, authzService.resolveActorTenantId(request),
                "CHANGE_ROLE", "USER", targetUserId, target.getTenantId(), before,
                "{\"role\":\"" + newRole + "\",\"level\":\"" + target.getLevel() + "\"}",
                null, request.getRemoteAddr());
    }

    private String beforeRoleOr(User target, String newRole) {
        return target.getRole() != null ? target.getRole() : newRole;
    }

    private void ensureNotLastTenantAdmin(Integer tenantId, Integer excludingUserId) {
        // Lightweight guard: count other TENANT_ADMIN in tenant via selectAll filter
        User probe = new User();
        probe.setTenantId(tenantId);
        probe.setRole(RoleEnum.TENANT_ADMIN.name());
        long others = userService.selectAll(probe).stream()
                .filter(u -> !u.getId().equals(excludingUserId))
                .count();
        if (others < 1) {
            throw new ApiException(ErrorType.CONFLICT, "Cannot demote the last TENANT_ADMIN");
        }
    }

    private String generateTempPassword() {
        byte[] bytes = new byte[18];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void queueTempPasswordEmail(String email, String tempPassword) {
        // Intentionally do not log tempPassword. Production wires encrypted outbox.
    }
}
