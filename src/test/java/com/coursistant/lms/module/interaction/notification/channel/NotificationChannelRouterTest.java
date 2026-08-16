package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.event.NotificationEvent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationChannelRouterTest {

    private final NotificationChannelRouter router = new NotificationChannelRouter();

    @Test
    void immediateEvents_doNotEnterDigest() {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED);
        Set<NotificationChannel> channels = router.channelsFor(event);
        assertTrue(channels.contains(NotificationChannel.IN_APP));
        assertTrue(channels.contains(NotificationChannel.IMMEDIATE_EMAIL));
        assertFalse(channels.contains(NotificationChannel.DAILY_DIGEST));
    }

    @Test
    void announcement_usesDigest() {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(NotificationType.ANNOUNCEMENT_POSTED);
        Set<NotificationChannel> channels = router.channelsFor(event);
        assertTrue(channels.contains(NotificationChannel.DAILY_DIGEST));
        assertFalse(channels.contains(NotificationChannel.IMMEDIATE_EMAIL));
    }

    @Test
    void unknownType_defaultsToDigest() {
        NotificationEvent event = new NotificationEvent();
        assertEquals(Set.of(NotificationChannel.IN_APP, NotificationChannel.DAILY_DIGEST),
                router.channelsFor(event));
    }

    @Test
    void snapshotTypes_requireExplicit() {
        router.assertRecipientMode(NotificationType.ASSIGNMENT_GRADE_RELEASED, RecipientMode.EXPLICIT);
        assertThrows(IllegalArgumentException.class,
                () -> router.assertRecipientMode(NotificationType.QUIZ_GRADE_CORRECTED,
                        RecipientMode.COURSE_ACTIVE_STUDENTS));
    }
}
