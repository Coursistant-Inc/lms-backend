package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.ExplicitRecipientValidator;
import com.coursistant.lms.module.interaction.notification.service.NotificationFanoutService;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.interaction.notification.support.NotificationLog;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!openapi")
public class NotificationEventRelayWorker {

    @Resource
    private NotificationEventOutboxMapper outboxMapper;

    @Resource
    private NotificationEventRecipientMapper recipientMapper;

    @Resource
    private NotificationFanoutService notificationFanoutService;

    @Resource
    private NotificationRecipientResolver notificationRecipientResolver;

    @Resource
    private ExplicitRecipientValidator explicitRecipientValidator;

    @Resource
    private NotificationClaimService claimService;

    @Resource
    private NotificationJson notificationJson;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationProperties notificationProperties;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Async("notificationExecutor")
    public void triggerFastPath(Long outboxId) {
        if (outboxId == null) {
            return;
        }
        try {
            processOne(outboxId);
        } catch (Exception e) {
            NotificationLog.warn("relay_fast_path_failed", null, null, null, null, null,
                    null, null, null, null, null, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${lms.notification.outbox.poll-ms:5000}")
    public void poll() {
        if (!notificationProperties.getOutbox().isEnabled()) {
            return;
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        List<Long> ids = outboxMapper.selectClaimBatch(now, notificationProperties.getOutbox().getBatchSize());
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            try {
                processOne(id);
            } catch (Exception e) {
                NotificationLog.warn("relay_poll_item_failed", null, null, null, null, null,
                        null, null, null, null, null, e.getMessage());
            }
        }
    }

    public void processOne(Long outboxId) {
        if (!notificationProperties.getOutbox().isEnabled()) {
            return;
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        LocalDateTime leaseUntil = now.plusSeconds(notificationProperties.getOutbox().getLeaseSeconds());
        var claimed = claimService.claimOutbox(outboxId, now, leaseUntil,
                notificationProperties.getOutbox().getMaxAttempts());
        if (claimed.isEmpty()) {
            return;
        }
        NotificationEventOutbox claimedRow = claimed.get().row();
        String token = claimed.get().token();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                NotificationEventOutbox row = outboxMapper.lockClaimed(outboxId, token, now);
                if (row == null) {
                    return;
                }
                List<Integer> recipients;
                try {
                    recipients = resolveRecipients(row);
                } catch (RuntimeException e) {
                    throw new RelayFailure("recipient_resolution_failed", e.getMessage(), e);
                }
                try {
                    notificationFanoutService.persist(toPayload(row), recipients);
                } catch (RuntimeException e) {
                    throw new RelayFailure("fanout_failed", e.getMessage(), e);
                }
                int done = outboxMapper.markDone(outboxId, token, now);
                if (done == 0) {
                    throw new RelayFailure("stale_claim", "markDone stale", null);
                }
            });
        } catch (RuntimeException e) {
            String event = e instanceof RelayFailure failure ? failure.event : "relay_failed";
            NotificationLog.warn(event, claimedRow.getEventId(), claimedRow.getTenantId(),
                    claimedRow.getNotificationType(), null, "FAILED_RETRYABLE",
                    FailureCategory.RETRYABLE_CHANNEL_PERSIST.name(), null, null,
                    claimedRow.getAttemptCount(), token,
                    e.getMessage() == null ? "fanout_failed" : e.getMessage());
            LocalDateTime retryNow = notificationTimeSupport.nowUtc();
            TransactionTemplate retry = new TransactionTemplate(transactionManager);
            retry.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            Integer retried = retry.execute(status -> outboxMapper.markRetryable(outboxId, token,
                    backoff(retryNow, claimedRow.getAttemptCount()),
                    NotificationLog.truncateLastError(e.getMessage() == null ? "fanout_failed" : e.getMessage()),
                    retryNow));
            if (retried == null || retried == 0) {
                NotificationLog.warn("stale_claim", claimedRow.getEventId(), claimedRow.getTenantId(),
                        claimedRow.getNotificationType(), null, claimedRow.getStatus(),
                        null, null, null, claimedRow.getAttemptCount(), token, "markRetryable");
            }
        }
    }

    private List<Integer> resolveRecipients(NotificationEventOutbox row) {
        RecipientMode mode = RecipientMode.valueOf(row.getRecipientMode());
        List<Integer> raw;
        if (mode == RecipientMode.EXPLICIT) {
            raw = loadExplicit(row.getId());
            List<Integer> validated = explicitRecipientValidator.validate(row.getTenantId(), raw);
            return excludeActor(NotificationType.valueOf(row.getNotificationType()), row.getActorUserId(), validated);
        }
        raw = notificationRecipientResolver.resolveActiveStudentRecipients(row.getCourseId());
        return excludeActor(NotificationType.valueOf(row.getNotificationType()), row.getActorUserId(), raw);
    }

    private List<Integer> loadExplicit(Long outboxId) {
        List<Integer> all = new ArrayList<>();
        int offset = 0;
        int chunk = notificationProperties.getOutbox().getRecipientInsertChunk();
        while (true) {
            List<Integer> page = recipientMapper.selectRecipientIds(outboxId, offset, chunk);
            if (page == null || page.isEmpty()) {
                break;
            }
            all.addAll(page);
            if (page.size() < chunk) {
                break;
            }
            offset += chunk;
        }
        return all;
    }

    private List<Integer> excludeActor(NotificationType type, Integer actorUserId, List<Integer> recipients) {
        if (recipients == null || recipients.isEmpty() || actorUserId == null) {
            return recipients == null ? List.of() : recipients;
        }
        List<Integer> filtered = new ArrayList<>();
        for (Integer id : recipients) {
            if (!explicitRecipientValidator.shouldExcludeActor(type, actorUserId, id)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    private NotificationDispatchPayload toPayload(NotificationEventOutbox row) {
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setEventId(row.getEventId());
        payload.setNotificationType(NotificationType.valueOf(row.getNotificationType()));
        payload.setEventKey(row.getEventKey());
        payload.setTenantId(row.getTenantId());
        payload.setCourseId(row.getCourseId());
        payload.setActorUserId(row.getActorUserId());
        payload.setRecipientMode(RecipientMode.valueOf(row.getRecipientMode()));
        payload.setSubjectType(SubjectType.valueOf(row.getSubjectType()));
        payload.setSubjectId(row.getSubjectId());
        payload.setMessage(row.getMessage());
        payload.setDeepLink(row.getDeepLink());
        payload.setCreatedAt(row.getOccurredAt());
        payload.setTemplateVars(notificationJson.readVars(row.getTemplateVarsJson()));
        return payload;
    }

    private LocalDateTime backoff(LocalDateTime now, Integer attempt) {
        int n = attempt == null ? 1 : attempt;
        long seconds = (long) Math.min(3600, Math.pow(notificationProperties.getEmail().getBackoffBaseSeconds(), n));
        return now.plusSeconds(Math.max(2, seconds));
    }

    private static final class RelayFailure extends RuntimeException {
        private final String event;

        private RelayFailure(String event, String message, Throwable cause) {
            super(message, cause);
            this.event = event;
        }
    }

}
