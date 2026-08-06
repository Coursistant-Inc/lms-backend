package com.coursistant.lms.shared.security;

/**
 * Authenticated caller for course (and future) authorization.
 * Built only from verified auth context — never from request body/query.
 */
public final class ActorContext {

    public static final String ACTOR_USER = "USER";
    public static final String ACTOR_ADMIN = "ADMIN";

    private final String actorType;
    private final Integer actorId;
    private final String role;
    private final Integer tenantId;
    private final String userLevel;
    private final String accountStatus;

    public ActorContext(String actorType, Integer actorId, String role,
                        Integer tenantId, String userLevel, String accountStatus) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.role = role;
        this.tenantId = tenantId;
        this.userLevel = userLevel;
        this.accountStatus = accountStatus;
    }

    public String getActorType() {
        return actorType;
    }

    public Integer getActorId() {
        return actorId;
    }

    public String getRole() {
        return role;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public String getUserLevel() {
        return userLevel;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public boolean isSystemAdmin() {
        return com.coursistant.lms.shared.enums.RoleEnum.SYSTEM_ADMIN.name().equals(role);
    }

    public boolean isTenantAdmin() {
        return com.coursistant.lms.shared.enums.RoleEnum.TENANT_ADMIN.name().equals(role);
    }

    public boolean isUser() {
        return com.coursistant.lms.shared.enums.RoleEnum.USER.name().equals(role);
    }
}
