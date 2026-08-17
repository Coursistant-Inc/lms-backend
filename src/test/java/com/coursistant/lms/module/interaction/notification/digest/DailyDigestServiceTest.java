package com.coursistant.lms.module.interaction.notification.digest;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.email.EmailSendResult;
import com.coursistant.lms.module.interaction.notification.email.NotificationContactLookup;
import com.coursistant.lms.module.interaction.notification.email.NotificationEmailSender;
import com.coursistant.lms.module.interaction.notification.email.NotificationEmailTemplateFactory;
import com.coursistant.lms.module.interaction.notification.email.RenderedEmail;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDigestEmail;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyDigestServiceTest {

    @Mock private NotificationDeliveryMapper deliveryMapper;
    @Mock private NotificationDigestEmailMapper digestEmailMapper;
    @Mock private NotificationContactLookup contactLookup;
    @Mock private NotificationClaimService claimService;
    @Mock private NotificationEmailTemplateFactory templateFactory;
    @Mock private NotificationEmailSender emailSender;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @InjectMocks private DailyDigestService service;

    private final LocalDateTime now = LocalDateTime.of(2026, 8, 16, 1, 0);
    private final LocalDate digestDate = LocalDate.of(2026, 8, 16);
    private final NotificationProperties properties = new NotificationProperties();

    @BeforeEach
    void init() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "notificationJson",
                new NotificationJson(new ObjectMapper()));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "notificationProperties",
                properties);
        properties.getEmail().setEnabled(true);
        org.mockito.Mockito.lenient().when(notificationTimeSupport.nowUtc()).thenReturn(now);
    }

    @Test
    void collectOne_collecting_attachesAndFreezes() {
        User user = enabledUser();
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(digestEmailMapper.selectByKey(1, 4, digestDate)).thenAnswer(inv -> {
            NotificationDigestEmail row = new NotificationDigestEmail();
            row.setId(20L);
            row.setStatus("COLLECTING");
            return row;
        });
        when(deliveryMapper.countByDigestEmailId(20L)).thenReturn(3);

        service.collectOne(1, 4, digestDate);

        verify(deliveryMapper).attachDigestItems(20L, digestDate, 1, 4);
        verify(digestEmailMapper).freezeCollected(eq(20L), eq(3), any());
        verify(digestEmailMapper, never()).markSkippedIneligible(any(), any());
    }

    @Test
    void collectOne_emptyItems_skipsIneligible() {
        User user = enabledUser();
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(digestEmailMapper.selectByKey(1, 4, digestDate)).thenAnswer(inv -> {
            NotificationDigestEmail row = new NotificationDigestEmail();
            row.setId(20L);
            row.setStatus("COLLECTING");
            return row;
        });
        when(deliveryMapper.countByDigestEmailId(20L)).thenReturn(0);

        service.collectOne(1, 4, digestDate);

        verify(digestEmailMapper).markSkippedIneligible(eq(20L), any());
        verify(digestEmailMapper, never()).freezeCollected(any(), anyInt(), any());
    }

    @Test
    void collectOne_alreadySent_bumpsLeftoverToNextDay() {
        User user = enabledUser();
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(digestEmailMapper.selectByKey(1, 4, digestDate)).thenAnswer(inv -> {
            NotificationDigestEmail row = new NotificationDigestEmail();
            row.setId(20L);
            row.setStatus("SENT");
            return row;
        });

        service.collectOne(1, 4, digestDate);

        verify(deliveryMapper).bumpUnattachedDigestDate(1, 4, digestDate, LocalDate.of(2026, 8, 17));
        verify(digestEmailMapper, never()).freezeCollected(any(), anyInt(), any());
    }

    @Test
    void collectOne_preferenceOff_skipsItemsWithoutCreatingSend() {
        User user = enabledUser();
        user.setEmailNotifications(false);
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(false);

        service.collectOne(1, 4, digestDate);

        verify(deliveryMapper).skipPendingDigestForRecipient(1, 4, digestDate);
        verify(digestEmailMapper, never()).insertCollecting(any());
    }

    @Test
    void run_requiresDigestDate() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.run(null, 1));
        verify(deliveryMapper, never()).selectPendingDigestRecipients(any(), any());
    }

    @Test
    void sendOne_truncatesLastErrorTo512() {
        NotificationDigestEmail row = new NotificationDigestEmail();
        row.setId(20L);
        row.setRecipientUserId(4);
        row.setTenantId(1);
        row.setDigestDate(digestDate);
        row.setAttemptCount(1);
        when(claimService.claimDigestEmail(eq(20L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(row, "tok")));
        User user = enabledUser();
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(contactLookup.hasUsableEmail(user)).thenReturn(true);
        when(contactLookup.accountActive(user)).thenReturn(true);
        when(deliveryMapper.selectByDigestEmailId(20L)).thenReturn(List.of(item("CS101", "Intro", "A")));
        when(templateFactory.renderDigest(any(), anyList())).thenReturn(new RenderedEmail("s", "b"));
        when(claimService.markDigestSendAttempted(eq(20L), eq("tok"), any(), any())).thenReturn(1);
        when(emailSender.send(any())).thenReturn(EmailSendResult.retryable(
                FailureCategory.RETRYABLE_NETWORK, "x".repeat(600)));

        service.sendOne(20L);

        org.mockito.ArgumentCaptor<String> error = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(digestEmailMapper).markRetry(eq(20L), eq("tok"), any(), any(), error.capture(), any());
        assertEquals(512, error.getValue().length());
    }

    @Test
    void sendOne_retryable_doesNotMarkItemsSent() {
        NotificationDigestEmail row = new NotificationDigestEmail();
        row.setId(20L);
        row.setRecipientUserId(4);
        row.setTenantId(1);
        row.setDigestDate(digestDate);
        row.setAttemptCount(1);
        when(claimService.claimDigestEmail(eq(20L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(row, "tok")));
        User user = enabledUser();
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(contactLookup.hasUsableEmail(user)).thenReturn(true);
        when(contactLookup.accountActive(user)).thenReturn(true);
        when(deliveryMapper.selectByDigestEmailId(20L)).thenReturn(List.of(item("CS101", "Intro", "A")));
        when(templateFactory.renderDigest(any(), anyList())).thenReturn(new RenderedEmail("s", "b"));
        when(claimService.markDigestSendAttempted(eq(20L), eq("tok"), any(), any())).thenReturn(1);
        when(emailSender.send(any())).thenReturn(EmailSendResult.retryable(
                FailureCategory.RETRYABLE_NETWORK, "smtp"));

        service.sendOne(20L);

        verify(digestEmailMapper).markRetry(eq(20L), eq("tok"), any(), any(), any(), any());
        verify(deliveryMapper, never()).markItemsByDigestEmailId(eq(20L), eq("SENT"), any());
        verify(claimService, never()).completeDigestTerminal(any(), any(), any(), any());
    }

    @Test
    void sendOne_preferenceOff_usesCompleteDigestTerminal() {
        NotificationDigestEmail row = new NotificationDigestEmail();
        row.setId(20L);
        row.setRecipientUserId(4);
        row.setTenantId(1);
        row.setAttemptCount(1);
        when(claimService.claimDigestEmail(eq(20L), any(), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(row, "tok")));
        User user = enabledUser();
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(false);
        when(claimService.completeDigestTerminal(eq(20L), eq("tok"), any(), any())).thenReturn(true);

        service.sendOne(20L);

        verify(claimService).completeDigestTerminal(eq(20L), eq("tok"), any(), any());
        verify(deliveryMapper, never()).markItemsByDigestEmailId(any(), any(), any());
        verify(emailSender, never()).send(any());
    }

    @Test
    void sendOne_usesRefreshedNowForMarker() {
        LocalDateTime t0 = LocalDateTime.of(2026, 8, 16, 1, 0);
        LocalDateTime t1 = t0.plusMinutes(3);
        when(notificationTimeSupport.nowUtc()).thenReturn(t0, t1, t1);
        NotificationDigestEmail row = new NotificationDigestEmail();
        row.setId(20L);
        row.setRecipientUserId(4);
        row.setTenantId(1);
        row.setDigestDate(digestDate);
        row.setAttemptCount(1);
        when(claimService.claimDigestEmail(eq(20L), eq(t0), any(), anyInt()))
                .thenReturn(Optional.of(new NotificationClaimService.Claimed<>(row, "tok")));
        User user = enabledUser();
        when(contactLookup.load(List.of(4))).thenReturn(Map.of(4, user));
        when(contactLookup.emailEnabled(user)).thenReturn(true);
        when(contactLookup.hasUsableEmail(user)).thenReturn(true);
        when(contactLookup.accountActive(user)).thenReturn(true);
        when(deliveryMapper.selectByDigestEmailId(20L)).thenReturn(List.of(item("CS101", "Intro", "A")));
        when(templateFactory.renderDigest(any(), anyList())).thenReturn(new RenderedEmail("s", "b"));
        when(claimService.markDigestSendAttempted(eq(20L), eq("tok"), eq(t1), eq(t1.plusSeconds(120))))
                .thenReturn(1);
        when(emailSender.send(any())).thenReturn(EmailSendResult.retryable(
                FailureCategory.RETRYABLE_NETWORK, "smtp"));

        service.sendOne(20L);

        verify(claimService).markDigestSendAttempted(20L, "tok", t1, t1.plusSeconds(120));
    }

    @Test
    void run_emailDisabled_skipsPhaseB() {
        properties.getEmail().setEnabled(false);
        when(deliveryMapper.selectPendingDigestRecipients(digestDate, 1)).thenReturn(List.of());

        service.run(digestDate, 1);

        verify(digestEmailMapper, never()).selectClaimBatch(any(), anyInt(), any(), any());
        verify(claimService, never()).claimDigestEmail(any(), any(), any(), anyInt());
    }

    @Test
    void groupItems_ordersByCourse() {
        NotificationDelivery a = item("CS101", "Intro", "A");
        NotificationDelivery b = item("CS101", "Intro", "B");
        NotificationDelivery c = item("MATH1", "Calc", "C");
        var groups = service.groupItems(List.of(a, b, c));
        assertEquals(2, groups.size());
        assertEquals("CS101", groups.get(0).courseCode());
        assertEquals(2, groups.get(0).lines().size());
    }

    private User enabledUser() {
        User user = new User();
        user.setId(4);
        user.setEmail("a@b.com");
        user.setEmailNotifications(true);
        user.setStatus("ACTIVE");
        return user;
    }

    private NotificationDelivery item(String code, String title, String message) {
        NotificationDelivery row = new NotificationDelivery();
        row.setMessage(message);
        row.setTemplateVarsJson("{\"courseCode\":\"" + code + "\",\"courseTitle\":\"" + title + "\"}");
        return row;
    }
}
