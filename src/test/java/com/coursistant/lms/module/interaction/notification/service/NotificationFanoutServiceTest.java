package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.digest.TenantTimeZoneResolver;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.email.NotificationContactLookup;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.UserNotificationMapper;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFanoutServiceTest {

    @Mock private UserNotificationMapper userNotificationMapper;
    @Mock private NotificationDeliveryMapper notificationDeliveryMapper;
    @Mock private NotificationContactLookup contactLookup;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @Mock private TenantTimeZoneResolver tenantTimeZoneResolver;
    @InjectMocks private NotificationFanoutService fanout;
    @Captor private ArgumentCaptor<List<NotificationDelivery>> rowsCaptor;

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(fanout, "notificationJson",
                new NotificationJson(new ObjectMapper()));
        org.springframework.test.util.ReflectionTestUtils.setField(fanout, "notificationProperties",
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

        fanout.persist(payload(), List.of(4));

        verify(userNotificationMapper).insertChunk(anyList());
        verify(notificationDeliveryMapper).upsertChunk(rowsCaptor.capture());
        assertEquals("SKIPPED_PREFERENCE", rowsCaptor.getValue().get(0).getStatus());
        assertEquals("IMMEDIATE_EMAIL", rowsCaptor.getValue().get(0).getChannel());
    }

    @Test
    void emailDisabledGlobally_writesInAppOnly() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setEnabled(false);
        org.springframework.test.util.ReflectionTestUtils.setField(fanout, "notificationProperties", properties);

        fanout.persist(payload(), List.of(4));

        verify(userNotificationMapper).insertChunk(anyList());
        verify(notificationDeliveryMapper, never()).upsertChunk(anyList());
    }

    private NotificationDispatchPayload payload() {
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setEventId("e1");
        payload.setNotificationType(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED);
        payload.setSubjectType(SubjectType.ASSIGNMENT_SUBMISSION);
        payload.setSubjectId(9);
        payload.setEventKey("submission:1");
        payload.setTenantId(1);
        payload.setCourseId(2);
        payload.setMessage("received");
        payload.setDeepLink("/x");
        return payload;
    }
}
