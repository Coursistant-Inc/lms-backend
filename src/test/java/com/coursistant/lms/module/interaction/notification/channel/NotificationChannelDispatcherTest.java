package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.enums.NotificationChannel;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationChannelDispatcherTest {

    @Mock private NotificationChannelRouter notificationChannelRouter;
    @Mock private InAppNotificationChannel inAppNotificationChannel;
    @Mock private EmailQueueChannel emailQueueChannel;
    @InjectMocks private NotificationChannelDispatcher dispatcher;

    @Test
    void emailFailure_doesNotPreventInAppPersistFlagIndependence() {
        NotificationEvent event = event();
        when(notificationChannelRouter.channelsFor(event)).thenReturn(EnumSet.of(
                NotificationChannel.IN_APP, NotificationChannel.IMMEDIATE_EMAIL));
        when(inAppNotificationChannel.persist(any(), anyList()))
                .thenReturn(new ChannelPersistResult(NotificationChannel.IN_APP, true, 1, null));
        when(emailQueueChannel.persistImmediate(any(), anyList()))
                .thenThrow(new RuntimeException("smtp down"));

        DispatchOutcome outcome = dispatcher.dispatch(event, List.of(1));
        assertFalse(outcome.allPersisted());
        assertTrue(outcome.failedChannels().contains(NotificationChannel.IMMEDIATE_EMAIL));
        verify(inAppNotificationChannel).persist(any(), anyList());
    }

    @Test
    void inAppFailure_stillAttemptsEmail() {
        NotificationEvent event = event();
        when(notificationChannelRouter.channelsFor(event)).thenReturn(EnumSet.of(
                NotificationChannel.IN_APP, NotificationChannel.IMMEDIATE_EMAIL));
        when(inAppNotificationChannel.persist(any(), anyList()))
                .thenReturn(new ChannelPersistResult(NotificationChannel.IN_APP, false, 0, "db"));
        when(emailQueueChannel.persistImmediate(any(), anyList()))
                .thenReturn(new ChannelPersistResult(NotificationChannel.IMMEDIATE_EMAIL, true, 1, null));

        DispatchOutcome outcome = dispatcher.dispatch(event, List.of(1));
        assertFalse(outcome.allPersisted());
        verify(emailQueueChannel).persistImmediate(any(), anyList());
        verify(emailQueueChannel, never()).persistDigest(any(), anyList());
    }

    private NotificationEvent event() {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(NotificationType.ASSIGNMENT_GRADE_RELEASED);
        event.setSubjectType(SubjectType.ASSIGNMENT);
        return event;
    }
}
