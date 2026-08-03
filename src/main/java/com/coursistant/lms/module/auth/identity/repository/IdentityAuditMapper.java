package com.coursistant.lms.module.auth.identity.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdentityAuditMapper {
    @Insert("INSERT INTO identity_audit (actor_id, actor_role, actor_tenant_id, action, target_type, target_id, " +
            "target_tenant_id, before_json, after_json, reason, result, ip, trace_id) " +
            "VALUES (#{actorId}, #{actorRole}, #{actorTenantId}, #{action}, #{targetType}, #{targetId}, " +
            "#{targetTenantId}, #{beforeJson}, #{afterJson}, #{reason}, #{result}, #{ip}, #{traceId})")
    int insert(@Param("actorId") Integer actorId,
               @Param("actorRole") String actorRole,
               @Param("actorTenantId") Integer actorTenantId,
               @Param("action") String action,
               @Param("targetType") String targetType,
               @Param("targetId") Integer targetId,
               @Param("targetTenantId") Integer targetTenantId,
               @Param("beforeJson") String beforeJson,
               @Param("afterJson") String afterJson,
               @Param("reason") String reason,
               @Param("result") String result,
               @Param("ip") String ip,
               @Param("traceId") String traceId);
}
