package com.coursistant.lms.module.interaction.notification.event;

import com.coursistant.lms.module.interaction.notification.channel.DispatchOutcome;
import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelDispatcher;
import com.coursistant.lms.module.interaction.notification.channel.NotificationChannelRouter;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventRecipientMapper;
import com.coursistant.lms.module.interaction.notification.service.ExplicitRecipientValidator;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.interaction.notification.support.NotificationLog;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Profile("!openapi")
public class NotificationEventRelayWorker {

    @Resource
    private NotificationEventOutboxMapper outboxMapper;

    @Resource
    private NotificationEventRecipientMapper recipientMapper;

    @Resource
    private NotificationChannelDispatcher channelDispatcher;

    @Resource
    private NotificationChannelRouter channelRouter;

    @Resource
    private NotificationRecipientResolver notificationRecipientResolver;

    @Resource
    private ExplicitRecipientValidator explicitRecipientValidator;

    @Resource
    private NotificationJson notificationJson;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationProperties notificationProperties;

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

    @Transactional
    public void processOne(Long outboxId) {
        if (!notificationProperties.getOutbox().isEnabled()) {
            return;
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        String token = UUID.randomUUID().toString();
        LocalDateTime leaseUntil = now.plusSeconds(notificationProperties.getOutbox().getLeaseSeconds());
        int claimed = outboxMapper.claim(outboxId, token, leaseUntil, now);
        if (claimed == 0) {
            return;
        }
        NotificationEventOutbox row = outboxMapper.selectById(outboxId);
        if (row == null) {
            return;
        }
        if (row.getAttemptCount() != null
                && row.getAttemptCount() > notificationProperties.getOutbox().getMaxAttempts()) {
            outboxMapper.markPermanent(outboxId, token, FailureCategory.ORPHAN_MAX_ATTEMPTS.name(), now);
            return;
        }
        NotificationEvent event = toEvent(row);
        List<Integer> recipients = resolveRecipients(row, event);
        DispatchOutcome outcome = channelDispatcher.dispatch(event, recipients);
        if (outcome.allPersisted()) {
            int done = outboxMapper.markDone(outboxId, token, now);
            if (done == 0) {
                NotificationLog.warn("stale_claim", event.getEventId(), event.getTenantId(),
                        event.getEventType().name(), null, "DONE", null, null, null,
                        row.getAttemptCount(), token, "markDone");
            }
        } else {
            NotificationLog.warn("channel_persist_incomplete", event.getEventId(), event.getTenantId(),
                    event.getEventType().name(), String.valueOf(outcome.failedChannels()),
                    "FAILED_RETRYABLE", FailureCategory.RETRYABLE_CHANNEL_PERSIST.name(),
                    null, null, row.getAttemptCount(), token, "retry");
            int updated = outboxMapper.markRetryable(outboxId, token, backoff(now, row.getAttemptCount()),
                    "channels_not_persisted=" + outcome.failedChannels(), now);
            if (updated == 0) {
                NotificationLog.warn("stale_claim", event.getEventId(), event.getTenantId(),
                        event.getEventType().name(), null, "FAILED_RETRYABLE", null, null, null,
                        row.getAttemptCount(), token, "markRetryable");
            }
        }
    }

    private List<Integer> resolveRecipients(NotificationEventOutbox row, NotificationEvent event) {
        RecipientMode mode = RecipientMode.valueOf(row.getRecipientMode());
        List<Integer> raw;
        if (mode == RecipientMode.EXPLICIT) {
            raw = loadExplicit(row.getId());
            List<Integer> validated = explicitRecipientValidator.validate(row.getTenantId(), raw);
            return excludeActor(event.getEventType(), event.getActorUserId(), validated);
        }
        raw = notificationRecipientResolver.resolveActiveStudentRecipients(row.getCourseId());
        return excludeActor(event.getEventType(), event.getActorUserId(), raw);
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

    private NotificationEvent toEvent(NotificationEventOutbox row) {
        NotificationEvent event = new NotificationEvent();
        event.setEventId(row.getEventId());
        event.setEventType(NotificationType.valueOf(row.getNotificationType()));
        event.setEventKey(row.getEventKey());
        event.setTenantId(row.getTenantId());
        event.setCourseId(row.getCourseId());
        event.setActorUserId(row.getActorUserId());
        event.setRecipientMode(RecipientMode.valueOf(row.getRecipientMode()));
        event.setSubjectType(SubjectType.valueOf(row.getSubjectType()));
        event.setSubjectId(row.getSubjectId());
        event.setMessage(row.getMessage());
        event.setDeepLink(row.getDeepLink());
        event.setOccurredAt(row.getOccurredAt());
        event.setTemplateVars(notificationJson.readVars(row.getTemplateVarsJson()));
        return event;
    }

    private LocalDateTime backoff(LocalDateTime now, Integer attempt) {
        int n = attempt == null ? 1 : attempt;
        long seconds = (long) Math.min(3600, Math.pow(notificationProperties.getEmail().getBackoffBaseSeconds(), n));
        return now.plusSeconds(Math.max(2, seconds));
    }
}
