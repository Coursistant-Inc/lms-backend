package com.coursistant.lms.module.course.group.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.group.entity.CourseGroup;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.repository.CourseGroupMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationEventKeys;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class GroupNotificationService {

    public static final String VARIANT_TARGET = "target";
    public static final String VARIANT_MEMBERS = "members";
    public static final String VARIANT_OLD_MEMBERS = "old-members";
    public static final String VARIANT_NEW_MEMBERS = "new-members";

    private static final Logger log = LoggerFactory.getLogger(GroupNotificationService.class);
    private static final String STATE_ARCHIVED = "Archived";

    @Resource
    private NotificationPublisher notificationPublisher;

    @Resource
    private NotificationMessageFactory notificationMessageFactory;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private CourseGroupMapper courseGroupMapper;

    @Resource
    private UserMapper userMapper;

    public void notifyAdded(Course course, Integer groupSetId, Integer groupId, Integer targetUserId,
                            String actorType, Integer actorUserId, Integer auditId, List<Integer> membersAfter) {
        if (skip(course, auditId, targetUserId, groupSetId)) {
            return;
        }
        String groupName = groupName(groupId);
        String userName = userName(targetUserId);
        publish(course, NotificationType.GROUP_MEMBER_ADDED, groupSetId, groupId, null, targetUserId,
                actorUserId, auditId, VARIANT_TARGET, List.of(targetUserId),
                notificationMessageFactory.groupMemberAddedTarget(groupName),
                vars(course, groupSetId, groupId, null, targetUserId, userName, groupName, null));
        List<Integer> members = memberAudience(membersAfter, targetUserId, actorType, actorUserId);
        if (!members.isEmpty()) {
            publish(course, NotificationType.GROUP_MEMBER_ADDED, groupSetId, groupId, null, targetUserId,
                    actorUserId, auditId, VARIANT_MEMBERS, members,
                    notificationMessageFactory.groupMemberAddedMembers(userName, groupName),
                    vars(course, groupSetId, groupId, null, targetUserId, userName, groupName, null));
        }
    }

    public void notifyRemoved(Course course, Integer groupSetId, Integer groupId, Integer targetUserId,
                              String actorType, Integer actorUserId, Integer auditId, List<Integer> remainingMembers) {
        if (skip(course, auditId, targetUserId, groupSetId)) {
            return;
        }
        String groupName = groupName(groupId);
        String userName = userName(targetUserId);
        publish(course, NotificationType.GROUP_MEMBER_REMOVED, groupSetId, groupId, null, targetUserId,
                actorUserId, auditId, VARIANT_TARGET, List.of(targetUserId),
                notificationMessageFactory.groupMemberRemovedTarget(groupName),
                vars(course, groupSetId, groupId, null, targetUserId, userName, groupName, null));
        List<Integer> members = memberAudience(remainingMembers, targetUserId, actorType, actorUserId);
        if (!members.isEmpty()) {
            publish(course, NotificationType.GROUP_MEMBER_REMOVED, groupSetId, groupId, null, targetUserId,
                    actorUserId, auditId, VARIANT_MEMBERS, members,
                    notificationMessageFactory.groupMemberRemovedMembers(userName, groupName),
                    vars(course, groupSetId, groupId, null, targetUserId, userName, groupName, null));
        }
    }

    public void notifyMoved(Course course, Integer groupSetId, Integer oldGroupId, Integer newGroupId,
                            Integer targetUserId, String actorType, Integer actorUserId, Integer auditId,
                            List<Integer> oldRemaining, List<Integer> newMembers) {
        if (skip(course, auditId, targetUserId, groupSetId)) {
            return;
        }
        String oldName = groupName(oldGroupId);
        String newName = groupName(newGroupId);
        String userName = userName(targetUserId);
        publish(course, NotificationType.GROUP_MEMBER_MOVED, groupSetId, newGroupId, oldGroupId, targetUserId,
                actorUserId, auditId, VARIANT_TARGET, List.of(targetUserId),
                notificationMessageFactory.groupMemberMovedTarget(oldName, newName),
                vars(course, groupSetId, newGroupId, oldGroupId, targetUserId, userName, newName, oldName));
        List<Integer> oldAudience = memberAudience(oldRemaining, targetUserId, actorType, actorUserId);
        if (!oldAudience.isEmpty()) {
            publish(course, NotificationType.GROUP_MEMBER_MOVED, groupSetId, newGroupId, oldGroupId, targetUserId,
                    actorUserId, auditId, VARIANT_OLD_MEMBERS, oldAudience,
                    notificationMessageFactory.groupMemberMovedOldMembers(userName, oldName),
                    vars(course, groupSetId, newGroupId, oldGroupId, targetUserId, userName, newName, oldName));
        }
        List<Integer> newAudience = memberAudience(newMembers, targetUserId, actorType, actorUserId);
        if (!newAudience.isEmpty()) {
            publish(course, NotificationType.GROUP_MEMBER_MOVED, groupSetId, newGroupId, oldGroupId, targetUserId,
                    actorUserId, auditId, VARIANT_NEW_MEMBERS, newAudience,
                    notificationMessageFactory.groupMemberMovedNewMembers(userName, newName),
                    vars(course, groupSetId, newGroupId, oldGroupId, targetUserId, userName, newName, oldName));
        }
    }

    private void publish(Course course, NotificationType type, Integer groupSetId, Integer groupId,
                         Integer oldGroupId, Integer targetUserId, Integer actorUserId, Integer auditId,
                         String variant, List<Integer> recipients, String message, Map<String, String> vars) {
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setTenantId(course.getTenantId());
        payload.setCourseId(course.getId());
        payload.setNotificationType(type);
        payload.setMessage(message);
        payload.setSubjectType(SubjectType.GROUP_SET);
        payload.setSubjectId(groupSetId);
        payload.setEventKey(eventKey(type, auditId, variant));
        payload.setDeepLink("/courses/" + course.getId() + "/groups/" + groupSetId);
        payload.setActorUserId(actorUserId);
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setRecipientIds(new ArrayList<>(recipients));
        payload.setCreatedAt(notificationTimeSupport.nowUtc());
        if (oldGroupId != null) {
            vars.put("oldGroupId", String.valueOf(oldGroupId));
        }
        if (groupId != null) {
            vars.put("groupId", String.valueOf(groupId));
        }
        payload.setTemplateVars(vars);
        notificationPublisher.publishInTransaction(payload);
        log.debug("Group notification published type={} auditId={} variant={} recipients={}",
                type, auditId, variant, recipients.size());
    }

    private String eventKey(NotificationType type, Integer auditId, String variant) {
        return switch (type) {
            case GROUP_MEMBER_ADDED -> NotificationEventKeys.groupAdded(auditId, variant);
            case GROUP_MEMBER_REMOVED -> NotificationEventKeys.groupRemoved(auditId, variant);
            case GROUP_MEMBER_MOVED -> NotificationEventKeys.groupMoved(auditId, variant);
            default -> throw new IllegalArgumentException("Unsupported group notification type " + type);
        };
    }

    private List<Integer> memberAudience(List<Integer> members, Integer targetUserId, String actorType,
                                         Integer actorUserId) {
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        if (members != null) {
            for (Integer id : members) {
                if (id != null) {
                    unique.add(id);
                }
            }
        }
        unique.remove(targetUserId);
        if (GroupMembershipAudit.ACTOR_USER.equals(actorType)
                && actorUserId != null
                && !actorUserId.equals(targetUserId)) {
            unique.remove(actorUserId);
        }
        return new ArrayList<>(unique);
    }

    private boolean skip(Course course, Integer auditId, Integer targetUserId, Integer groupSetId) {
        if (course == null || STATE_ARCHIVED.equals(course.getState()) || course.getArchivedAt() != null) {
            log.debug("Skip group notification for archived course: targetUserId={}, groupSetId={}",
                    targetUserId, groupSetId);
            return true;
        }
        return auditId == null || targetUserId == null || groupSetId == null || course.getTenantId() == null;
    }

    private String groupName(Integer groupId) {
        if (groupId == null) {
            return "Group";
        }
        CourseGroup group = courseGroupMapper.selectById(groupId);
        if (group == null || group.getName() == null || group.getName().isBlank()) {
            return "Group";
        }
        return group.getName().trim();
    }

    private String userName(Integer userId) {
        if (userId == null) {
            return "A student";
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "A student";
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return "A student";
    }

    private Map<String, String> vars(Course course, Integer groupSetId, Integer groupId, Integer oldGroupId,
                                     Integer targetUserId, String userName, String groupName, String oldGroupName) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("courseCode", course.getCourseCode() == null ? "" : course.getCourseCode());
        vars.put("courseTitle", course.getTitle() == null ? "" : course.getTitle());
        vars.put("deepLink", "/courses/" + course.getId() + "/groups/" + groupSetId);
        vars.put("groupSetId", String.valueOf(groupSetId));
        vars.put("targetUserId", String.valueOf(targetUserId));
        vars.put("userName", userName);
        vars.put("groupName", groupName);
        if (oldGroupName != null) {
            vars.put("oldGroupName", oldGroupName);
        }
        if (oldGroupId != null) {
            vars.put("oldGroupId", String.valueOf(oldGroupId));
        }
        if (groupId != null) {
            vars.put("groupId", String.valueOf(groupId));
        }
        return vars;
    }
}
