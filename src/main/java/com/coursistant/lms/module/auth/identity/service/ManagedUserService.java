package com.coursistant.lms.module.auth.identity.service;

import com.coursistant.lms.module.course.enrollment.service.EnrollmentIdentityGuard;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.account.service.UserService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
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

    public record ResolvedRoleLevel(String role, String level) {}

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
    @Resource
    private EnrollmentIdentityGuard enrollmentIdentityGuard;
    @Resource
    private ActorContextResolver actorContextResolver;

    public record CreateManagedUserCommand(String email, String name, String role, String level, Integer tenantId) {}

    /**
     * Shared role/level rules for create and changeRole.
     * USER: level defaults to STUDENT; only STUDENT or INSTRUCTOR allowed.
     * TENANT_ADMIN: level forced to NOT_APPLICABLE; any other explicit level → 400.
     * SYSTEM_ADMIN: forbidden.
     */
    public ResolvedRoleLevel resolveRoleAndLevel(String role, String level) {
        if (RoleEnum.SYSTEM_ADMIN.name().equals(role)) {
            throw new ApiException(ErrorType.FORBIDDEN, "Cannot assign SYSTEM_ADMIN via user management");
        }
        if (!RoleEnum.USER.name().equals(role) && !RoleEnum.TENANT_ADMIN.name().equals(role)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Invalid role");
        }
        if (RoleEnum.TENANT_ADMIN.name().equals(role)) {
            if (level != null && !level.isBlank() && !LevelEnum.NOT_APPLICABLE.level.equals(level)) {
                throw new ApiException(ErrorType.BAD_REQUEST, "TENANT_ADMIN requires NOT_APPLICABLE level");
            }
            return new ResolvedRoleLevel(role, LevelEnum.NOT_APPLICABLE.level);
        }
        String resolved = (level == null || level.isBlank()) ? LevelEnum.STUDENT.level : level;
        if (!LevelEnum.STUDENT.level.equals(resolved) && !LevelEnum.INSTRUCTOR.level.equals(resolved)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "USER requires INSTRUCTOR or STUDENT level");
        }
        return new ResolvedRoleLevel(role, resolved);
    }

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

        ResolvedRoleLevel resolved = resolveRoleAndLevel(cmd.role(), cmd.level());

        Integer tenantId = systemScope ? cmd.tenantId() : actorTenantId;
        if (tenantId == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "tenantId is required");
        }
        if (!systemScope && !tenantId.equals(actorTenantId)) {
            throw new ApiException(ErrorType.NOT_FOUND);
        }

        String tempPassword = generateTempPassword();
        User user = new User();
        user.setEmail(AccountIdentityService.normalizeEmail(cmd.email()));
        user.setName(cmd.name());
        user.setUsername(user.getEmail().split("@")[0]);
        user.setRole(resolved.role());
        user.setLevel(resolved.level());
        user.setTenantId(tenantId);
        user.setEncryptPassword(tempPassword);
        user.setMustChangePassword(true);
        userMapper.insert(user);

        accountIdentityService.claimEmail(user.getEmail(), AccountIdentityService.PRINCIPAL_USER, user.getId());
        identityAuditService.writeSuccess(actorId, actorRole, actorTenantId, "CREATE_USER", "USER",
                user.getId(), tenantId, null,
                "{\"role\":\"" + resolved.role() + "\",\"level\":\"" + resolved.level() + "\"}",
                null, request.getRemoteAddr());

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

        ResolvedRoleLevel resolved = resolveRoleAndLevel(newRole, newLevelIfUser);

        if (RoleEnum.TENANT_ADMIN.name().equals(target.getRole())
                && RoleEnum.USER.name().equals(resolved.role())) {
            ensureNotLastTenantAdmin(target.getTenantId(), targetUserId);
        }

        User locked = enrollmentIdentityGuard.assertCanChangeRoleOrLevel(
                targetUserId, resolved.role(), resolved.level());

        String before = "{\"role\":\"" + locked.getRole() + "\",\"level\":\"" + locked.getLevel() + "\"}";
        locked.setRole(resolved.role());
        locked.setLevel(resolved.level());
        userMapper.updateById(locked);
        target = locked;
        sessionInvalidationService.invalidatePrincipal(targetUserId, beforeRoleOr(target, resolved.role()));
        sessionInvalidationService.invalidatePrincipal(targetUserId, RoleEnum.USER.name());
        sessionInvalidationService.invalidatePrincipal(targetUserId, RoleEnum.TENANT_ADMIN.name());

        identityAuditService.writeSuccess(actorId, actorRole, authzService.resolveActorTenantId(request),
                "CHANGE_ROLE", "USER", targetUserId, target.getTenantId(), before,
                "{\"role\":\"" + resolved.role() + "\",\"level\":\"" + target.getLevel() + "\"}",
                null, request.getRemoteAddr());
    }

    @Transactional
    public void disableUser(HttpServletRequest request, Integer targetUserId) {
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
        ActorContext actor = actorContextResolver.resolve(request);
        String before = "{\"status\":\"" + target.getStatus() + "\"}";
        enrollmentIdentityGuard.disableAccountWithEnrollmentWithdraw(actor, targetUserId,
                request.getHeader("Idempotency-Key"));
        User locked = userMapper.selectById(targetUserId);
        locked.setStatus(AccountStatus.DISABLED.name());
        userMapper.updateById(locked);
        // Bypass UserService.update would leave stale Redis user:{id} with ACTIVE and keep access tokens alive.
        userService.evictUserCache(locked.getId(), locked.getEmail());
        sessionInvalidationService.invalidatePrincipal(targetUserId, RoleEnum.USER.name());
        sessionInvalidationService.invalidatePrincipal(targetUserId, RoleEnum.TENANT_ADMIN.name());
        identityAuditService.writeSuccess(actorId, actorRole, authzService.resolveActorTenantId(request),
                "DISABLE_USER", "USER", targetUserId, locked.getTenantId(), before,
                "{\"status\":\"DISABLED\"}", null, request.getRemoteAddr());
    }

    private String beforeRoleOr(User target, String newRole) {
        return target.getRole() != null ? target.getRole() : newRole;
    }

    private void ensureNotLastTenantAdmin(Integer tenantId, Integer excludingUserId) {
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
}
