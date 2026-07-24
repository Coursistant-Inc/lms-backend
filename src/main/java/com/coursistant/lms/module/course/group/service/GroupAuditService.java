package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.group.entity.GroupMembership;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.repository.GroupMembershipAuditMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GroupAuditService {

    private static final Logger log = LoggerFactory.getLogger(GroupAuditService.class);

    @Resource
    private GroupMembershipAuditMapper groupMembershipAuditMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void write(Integer tenantId,
                      Integer courseId,
                      Integer groupSetId,
                      Integer groupId,
                      Integer targetUserId,
                      String actorType,
                      Integer actorUserId,
                      String action,
                      GroupMembership before,
                      GroupMembership after,
                      Map<String, ?> detail) {
        GroupMembershipAudit audit = new GroupMembershipAudit();
        audit.setTenantId(tenantId);
        audit.setCourseId(courseId);
        audit.setGroupSetId(groupSetId);
        audit.setGroupId(groupId);
        audit.setTargetUserId(targetUserId);
        audit.setActorType(actorType);
        audit.setActorUserId(actorUserId);
        audit.setAction(action);
        audit.setBeforeJson(toMembershipJson(before));
        audit.setAfterJson(toMembershipJson(after));
        audit.setDetailJson(toJson(detail));
        groupMembershipAuditMapper.insert(audit);
    }

    public String toMembershipJson(GroupMembership membership) {
        if (membership == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("groupId", membership.getGroupId());
        map.put("userId", membership.getUserId());
        map.put("joinedAt", membership.getJoinedAt() != null ? membership.getJoinedAt().toString() : null);
        map.put("addedByType", membership.getAddedByType());
        map.put("addedByUserId", membership.getAddedByUserId());
        return toJson(map);
    }

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize group audit payload: {}", e.getMessage());
            return null;
        }
    }
}
