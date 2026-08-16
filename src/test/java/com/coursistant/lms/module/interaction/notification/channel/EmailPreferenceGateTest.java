package com.coursistant.lms.module.interaction.notification.channel;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.digest.TenantTimeZoneResolver;
import com.coursistant.lms.module.interaction.notification.email.NotificationContactLookup;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationEvent;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.user.account.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailPreferenceGateTest {

    @Mock private NotificationDeliveryMapper notificationDeliveryMapper;
    @Mock private NotificationContactLookup contactLookup;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @Mock private TenantTimeZoneResolver tenantTimeZoneResolver;
    @InjectMocks private EmailQueueChannel channel;
    @Captor private ArgumentCaptor<List<NotificationDelivery>> rowsCaptor;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(channel, "notificationJson",
                new NotificationJson(new ObjectMapper()));
        org.springframework.test.util.ReflectionTestUtils.setField(channel, "notificationProperties",
                new NotificationProperties());
        org.mockito.Mockito.lenient().when(notificationTimeSupport.nowUtc())
                .thenReturn(LocalDateTime.of(2026, 8, 16, 1, 0));
    }

    @Test
    void preferenceOff_enqueuesSkippedPreference() {
        User user = new User();
        user.setId(4);
        user.setEmail("a@b.com");
        user.setEmailNotifications(false);
        user.setStatus("ACTIVE");
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.accountActive(user)).thenReturn(true);
        when(contactLookup.emailEnabled(user)).thenReturn(false);

        ChannelPersistResult result = channel.persistImmediate(event(), List.of(4));

        assertTrue(result.persisted());
        verify(notificationDeliveryMapper).upsertChunk(rowsCaptor.capture());
        assertEquals("SKIPPED_PREFERENCE", rowsCaptor.getValue().get(0).getStatus());
    }

    @Test
    void emailDisabledGlobally_persistsNothing() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setEnabled(false);
        org.springframework.test.util.ReflectionTestUtils.setField(channel, "notificationProperties", properties);

        ChannelPersistResult result = channel.persistImmediate(event(), List.of(4));

        assertTrue(result.persisted());
        assertEquals(0, result.rows());
        verify(notificationDeliveryMapper, never()).upsertChunk(anyList());
    }

    private NotificationEvent event() {
        NotificationEvent event = new NotificationEvent();
        event.setEventId("e1");
        event.setEventType(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED);
        event.setSubjectType(SubjectType.ASSIGNMENT_SUBMISSION);
        event.setSubjectId(9);
        event.setEventKey("submission:1");
        event.setTenantId(1);
        event.setCourseId(2);
        event.setMessage("received");
        event.setDeepLink("/x");
        return event;
    }
}
