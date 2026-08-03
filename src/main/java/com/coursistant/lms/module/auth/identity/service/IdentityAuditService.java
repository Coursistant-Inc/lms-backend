package com.coursistant.lms.module.auth.identity.service;

import com.coursistant.lms.module.auth.identity.repository.IdentityAuditMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityAuditService {

    @Resource
    private IdentityAuditMapper identityAuditMapper;

    @Transactional
    public void writeSuccess(Integer actorId, String actorRole, Integer actorTenantId,
                             String action, String targetType, Integer targetId, Integer targetTenantId,
                             String beforeJson, String afterJson, String reason, String ip) {
        identityAuditMapper.insert(actorId, actorRole, actorTenantId, action, targetType, targetId,
                targetTenantId, beforeJson, afterJson, reason, "SUCCESS", ip, null);
    }
}
