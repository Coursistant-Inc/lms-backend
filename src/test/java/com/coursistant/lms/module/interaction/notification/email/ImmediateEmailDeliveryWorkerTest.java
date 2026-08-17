package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.user.account.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImmediateEmailDeliveryWorkerTest {

    @Mock private NotificationDeliveryMapper deliveryMapper;
    @Mock private NotificationClaimService claimService;
    @Mock private NotificationContactLookup contactLookup;
    @Mock private NotificationEmailTemplateFactory templateFactory;
    @Mock private NotificationEmailSender emailSender;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @InjectMocks private ImmediateEmailDeliveryWorker worker;

    @BeforeEach
    void init() {
        NotificationProperties properties = new NotificationProperties();
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "notificationProperties", properties);
        org.springframework.test.util.ReflectionTestUtils.setField(worker, "notificationJson",
                new NotificationJson(new ObjectMapper()));
        when(notificationTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 16, 1, 0));
    }

    @Test
    void markSendAttemptedZero_doesNotCallSender() {
        NotificationDelivery row = delivery();
        when(claimService.claimDelivery(eq(9L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(row, "tok")));
        User user = new User();
        user.setId(4);
        user.setEmail("a@b.com");
        user.setEmailNotifications(true);
        user.setStatus("ACTIVE");
        when(contactLookup.load(anyList())).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(contactLookup.hasUsableEmail(user)).thenReturn(true);
        when(contactLookup.accountActive(user)).thenReturn(true);
        when(templateFactory.renderImmediate(any(), any())).thenReturn(new RenderedEmail("s", "b"));
        when(claimService.markDeliverySendAttempted(9L, "tok", LocalDateTime.of(2026, 8, 16, 1, 0))).thenReturn(0);

        worker.processOne(9L);

        verify(emailSender, never()).send(any());
    }

    @Test
    void preferenceOff_skipsWithoutSend() {
        NotificationDelivery row = delivery();
        when(claimService.claimDelivery(eq(9L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(row, "tok")));
        User user = new User();
        user.setId(4);
        when(contactLookup.load(anyList())).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(false);
        when(deliveryMapper.markSkipped(eq(9L), eq("tok"), anyString(), anyString(), any())).thenReturn(1);

        worker.processOne(9L);

        verify(emailSender, never()).send(any());
        verify(deliveryMapper).markSkipped(eq(9L), eq("tok"), eq("SKIPPED_PREFERENCE"), anyString(), any());
    }

    @Test
    void retryableFailure_marksRetry() {
        NotificationDelivery row = delivery();
        when(claimService.claimDelivery(eq(9L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(row, "tok")));
        User user = new User();
        user.setId(4);
        user.setEmail("a@b.com");
        user.setEmailNotifications(true);
        user.setStatus("ACTIVE");
        when(contactLookup.load(anyList())).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(contactLookup.hasUsableEmail(user)).thenReturn(true);
        when(contactLookup.accountActive(user)).thenReturn(true);
        when(templateFactory.renderImmediate(any(), any())).thenReturn(new RenderedEmail("s", "b"));
        when(claimService.markDeliverySendAttempted(9L, "tok", LocalDateTime.of(2026, 8, 16, 1, 0))).thenReturn(1);
        when(emailSender.send(any())).thenReturn(EmailSendResult.retryable(
                com.coursistant.lms.module.interaction.notification.enums.FailureCategory.RETRYABLE_NETWORK, "down"));
        when(deliveryMapper.markRetry(eq(9L), eq("tok"), any(), anyString(), anyString(), any())).thenReturn(1);

        worker.processOne(9L);

        verify(deliveryMapper).markRetry(eq(9L), eq("tok"), any(),
                eq("RETRYABLE_NETWORK"), anyString(), any());
        verify(deliveryMapper, never()).markSent(any(), any(), any(), any());
    }

    private NotificationDelivery delivery() {
        NotificationDelivery row = new NotificationDelivery();
        row.setId(9L);
        row.setRecipientUserId(4);
        row.setNotificationType("ASSIGNMENT_SUBMISSION_RECEIVED");
        row.setChannel("IMMEDIATE_EMAIL");
        row.setTenantId(1);
        row.setEventId("e1");
        row.setAttemptCount(1);
        row.setTemplateVarsJson("{\"courseCode\":\"CS\"}");
        return row;
    }
}
