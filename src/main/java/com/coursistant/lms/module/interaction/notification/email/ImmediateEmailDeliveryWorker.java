package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.interaction.notification.support.NotificationLog;
import com.coursistant.lms.module.user.account.entity.User;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Profile("!openapi")
public class ImmediateEmailDeliveryWorker {

    @Resource
    private NotificationDeliveryMapper deliveryMapper;

    @Resource
    private NotificationClaimService claimService;

    @Resource
    private NotificationContactLookup contactLookup;

    @Resource
    private NotificationEmailTemplateFactory templateFactory;

    @Resource
    private NotificationEmailSender emailSender;

    @Resource
    private NotificationJson notificationJson;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationProperties notificationProperties;

    @Scheduled(fixedDelayString = "${lms.notification.email.poll-ms:5000}")
    public void processBatch() {
        if (!notificationProperties.getEmail().isEnabled()) {
            return;
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        List<Long> ids = deliveryMapper.selectClaimBatch("IMMEDIATE_EMAIL", now,
                notificationProperties.getEmail().getBatchSize());
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            try {
                processOne(id);
            } catch (Exception e) {
                NotificationLog.warn("email_worker_item_failed", null, null, null, "IMMEDIATE_EMAIL",
                        null, null, null, null, null, null, e.getMessage());
            }
        }
    }

    public void processOne(Long id) {
        if (!notificationProperties.getEmail().isEnabled()) {
            return;
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        LocalDateTime leaseUntil = now.plusSeconds(notificationProperties.getEmail().getLeaseSeconds());
        Optional<NotificationClaimService.Claimed<NotificationDelivery>> claimed =
                claimService.claimDelivery(id, now, leaseUntil, notificationProperties.getEmail().getMaxAttempts());
        if (claimed.isEmpty()) {
            return;
        }
        NotificationDelivery row = claimed.get().row();
        String token = claimed.get().token();
        User user = contactLookup.load(List.of(row.getRecipientUserId())).get(row.getRecipientUserId());
        now = notificationTimeSupport.nowUtc();
        if (!contactLookup.emailEnabled(user)) {
            int n = deliveryMapper.markSkipped(id, token, "SKIPPED_PREFERENCE",
                    FailureCategory.PERMANENT_PREFERENCE.name(), now);
            if (n == 0) {
                stale(row, token, "preference");
            }
            return;
        }
        if (!contactLookup.hasUsableEmail(user) || !contactLookup.accountActive(user)) {
            int n = deliveryMapper.markPermanent(id, token, FailureCategory.PERMANENT_NO_EMAIL.name(),
                    "no-email", now);
            if (n == 0) {
                stale(row, token, "no-email");
            }
            return;
        }
        RenderedEmail rendered;
        try {
            Map<String, String> vars = notificationJson.readVars(row.getTemplateVarsJson());
            rendered = templateFactory.renderImmediate(NotificationType.valueOf(row.getNotificationType()), vars);
        } catch (Exception e) {
            deliveryMapper.markPermanent(id, token, FailureCategory.PERMANENT_MISSING_TEMPLATE.name(),
                    "template", now);
            return;
        }
        now = notificationTimeSupport.nowUtc();
        leaseUntil = now.plusSeconds(notificationProperties.getEmail().getLeaseSeconds());
        if (claimService.markDeliverySendAttempted(id, token, now, leaseUntil) == 0) {
            NotificationLog.warn("stale_claim", row.getEventId(), row.getTenantId(),
                    row.getNotificationType(), row.getChannel(), "PROCESSING",
                    null, null, row.getRecipientUserId(), row.getAttemptCount(), token,
                    "skip-smtp");
            return;
        }
        EmailSendResult result = emailSender.send(new EmailMessage(
                row.getRecipientUserId(), user.getEmail(), rendered.subject(), rendered.textBody()));
        LocalDateTime resultNow = notificationTimeSupport.nowUtc();
        if (result.status() == EmailSendStatus.UNKNOWN_OUTCOME) {
            NotificationLog.warn("unknown_outcome", row.getEventId(), row.getTenantId(),
                    row.getNotificationType(), row.getChannel(), "PROCESSING",
                    FailureCategory.UNKNOWN_OUTCOME.name(), null, row.getRecipientUserId(),
                    row.getAttemptCount(), token,
                    result.errorMessage() == null ? "smtp-unknown-outcome" : result.errorMessage());
            return;
        }
        applyResult(row, token, result, resultNow);
    }

    private void applyResult(NotificationDelivery row, String token, EmailSendResult result, LocalDateTime now) {
        int n = switch (result.status()) {
            case SENT -> deliveryMapper.markSent(row.getId(), token, result.providerMessageId(), now);
            case DRY_RUN -> deliveryMapper.markDryRun(row.getId(), token, result.providerMessageId(), now);
            case RETRYABLE_FAILURE -> deliveryMapper.markRetry(row.getId(), token,
                    backoff(now, row.getAttemptCount()),
                    result.failureCategory() == null ? FailureCategory.RETRYABLE_NETWORK.name()
                            : result.failureCategory().name(),
                    NotificationLog.truncateLastError(result.errorMessage()), now);
            case PERMANENT_FAILURE -> deliveryMapper.markPermanent(row.getId(), token,
                    result.failureCategory() == null ? FailureCategory.PERMANENT_NO_EMAIL.name()
                            : result.failureCategory().name(),
                    NotificationLog.truncateLastError(result.errorMessage()), now);
            case UNKNOWN_OUTCOME -> 1;
        };
        if (n == 0) {
            stale(row, token, "applyResult");
        }
    }

    private LocalDateTime backoff(LocalDateTime now, Integer attempt) {
        int n = attempt == null ? 1 : attempt;
        long seconds = (long) Math.min(3600,
                Math.pow(notificationProperties.getEmail().getBackoffBaseSeconds(), n));
        return now.plusSeconds(Math.max(2, seconds));
    }

    private void stale(NotificationDelivery row, String token, String detail) {
        NotificationLog.warn("stale_claim", row.getEventId(), row.getTenantId(),
                row.getNotificationType(), row.getChannel(), row.getStatus(),
                null, null, row.getRecipientUserId(), row.getAttemptCount(), token, detail);
    }
}
