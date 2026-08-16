package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.event.NotificationEvent;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class NotificationChannelRouter {

    private static final Set<NotificationType> SNAPSHOT_TYPES = EnumSet.of(
            NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED,
            NotificationType.ASSIGNMENT_GRADE_RELEASED,
            NotificationType.ASSIGNMENT_GRADE_CORRECTED,
            NotificationType.QUIZ_GRADE_RELEASED,
            NotificationType.QUIZ_GRADE_CORRECTED
    );

    private static final Set<NotificationType> RULE_TYPES = EnumSet.of(
            NotificationType.ANNOUNCEMENT_POSTED,
            NotificationType.ASSIGNMENT_PUBLISHED
    );

    private static final Map<NotificationType, Set<NotificationChannel>> ROUTES;

    static {
        EnumMap<NotificationType, Set<NotificationChannel>> map = new EnumMap<>(NotificationType.class);
        Set<NotificationChannel> immediate = Collections.unmodifiableSet(
                EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.IMMEDIATE_EMAIL));
        Set<NotificationChannel> digest = Collections.unmodifiableSet(
                EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.DAILY_DIGEST));
        map.put(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED, immediate);
        map.put(NotificationType.ASSIGNMENT_GRADE_RELEASED, immediate);
        map.put(NotificationType.ASSIGNMENT_GRADE_CORRECTED, immediate);
        map.put(NotificationType.QUIZ_GRADE_RELEASED, immediate);
        map.put(NotificationType.QUIZ_GRADE_CORRECTED, immediate);
        map.put(NotificationType.ANNOUNCEMENT_POSTED, digest);
        map.put(NotificationType.ASSIGNMENT_PUBLISHED, digest);
        ROUTES = Collections.unmodifiableMap(map);
    }

    public Set<NotificationChannel> channelsFor(NotificationEvent event) {
        if (event != null && event.getChannelPolicy() != null && !event.getChannelPolicy().isEmpty()) {
            return event.getChannelPolicy();
        }
        if (event == null || event.getEventType() == null) {
            return EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.DAILY_DIGEST);
        }
        return ROUTES.getOrDefault(event.getEventType(),
                EnumSet.of(NotificationChannel.IN_APP, NotificationChannel.DAILY_DIGEST));
    }

    public RecipientMode requiredRecipientMode(NotificationType type) {
        if (type != null && SNAPSHOT_TYPES.contains(type)) {
            return RecipientMode.EXPLICIT;
        }
        if (type != null && RULE_TYPES.contains(type)) {
            return RecipientMode.COURSE_ACTIVE_STUDENTS;
        }
        return RecipientMode.COURSE_ACTIVE_STUDENTS;
    }

    public void assertRecipientMode(NotificationType type, RecipientMode mode) {
        if (type != null && SNAPSHOT_TYPES.contains(type) && mode != RecipientMode.EXPLICIT) {
            throw new IllegalArgumentException(
                    "Snapshot notification type " + type + " must use EXPLICIT recipients");
        }
    }

    public boolean isSnapshotType(NotificationType type) {
        return type != null && SNAPSHOT_TYPES.contains(type);
    }

    public boolean isImmediateEmailType(NotificationType type) {
        Set<NotificationChannel> channels = ROUTES.get(type);
        return channels != null && channels.contains(NotificationChannel.IMMEDIATE_EMAIL);
    }
}
