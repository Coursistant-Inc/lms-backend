package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.enums.AccountStatus;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ExplicitRecipientValidator {

    private static final Logger log = LoggerFactory.getLogger(ExplicitRecipientValidator.class);

    @Resource
    private UserMapper userMapper;

    public List<Integer> validate(Integer tenantId, List<Integer> candidateIds) {
        try {
            return validateInternal(tenantId, candidateIds);
        } catch (Exception e) {
            log.warn("Explicit recipient validation failed; skipping. tenantId={} size={}",
                    tenantId, candidateIds == null ? 0 : candidateIds.size(), e);
            return Collections.emptyList();
        }
    }

    private List<Integer> validateInternal(Integer tenantId, List<Integer> candidateIds) {
        if (tenantId == null || candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> uniqueIds = new HashSet<>();
        List<Integer> ordered = new ArrayList<>();
        for (Integer id : candidateIds) {
            if (id != null && uniqueIds.add(id)) {
                ordered.add(id);
            }
        }
        if (ordered.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectUsersByIds(ordered);
        Set<Integer> valid = new HashSet<>();
        if (users != null) {
            for (User user : users) {
                if (user == null || user.getId() == null) {
                    continue;
                }
                if (!tenantId.equals(user.getTenantId())) {
                    continue;
                }
                if (user.getStatus() != null && !AccountStatus.ACTIVE.name().equals(user.getStatus())) {
                    continue;
                }
                valid.add(user.getId());
            }
        }
        List<Integer> result = new ArrayList<>();
        for (Integer id : ordered) {
            if (valid.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    public boolean shouldExcludeActor(NotificationType type, Integer actorUserId, Integer recipientId) {
        if (actorUserId == null || recipientId == null || !actorUserId.equals(recipientId)) {
            return false;
        }
        return type != NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED;
    }
}
