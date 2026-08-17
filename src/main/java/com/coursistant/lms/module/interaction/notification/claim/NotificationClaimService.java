package com.coursistant.lms.module.interaction.notification.claim;

import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDigestEmail;
import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationEventOutboxMapper;
import com.coursistant.lms.module.interaction.notification.support.NotificationLog;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationClaimService {

    @Resource
    private NotificationDeliveryMapper deliveryMapper;

    @Resource
    private NotificationDigestEmailMapper digestEmailMapper;

    @Resource
    private NotificationEventOutboxMapper outboxMapper;

    @Transactional
    public Optional<Claimed<NotificationEventOutbox>> claimOutbox(Long id, LocalDateTime now,
                                                                 LocalDateTime leaseUntil, int maxAttempts) {
        String token = UUID.randomUUID().toString();
        int claimed = outboxMapper.claim(id, token, leaseUntil, now);
        if (claimed == 0) {
            return Optional.empty();
        }
        NotificationEventOutbox row = outboxMapper.selectById(id);
        if (row == null) {
            return Optional.empty();
        }
        if (row.getAttemptCount() != null && row.getAttemptCount() > maxAttempts) {
            outboxMapper.markPermanent(id, token, FailureCategory.ORPHAN_MAX_ATTEMPTS.name(), now);
            return Optional.empty();
        }
        return Optional.of(new Claimed<>(row, token));
    }

    @Transactional
    public Optional<Claimed<NotificationDelivery>> claimDelivery(Long id, LocalDateTime now,
                                                                 LocalDateTime leaseUntil, int maxAttempts) {
        String token = UUID.randomUUID().toString();
        int claimed = deliveryMapper.claim(id, token, leaseUntil, now);
        if (claimed == 0) {
            return Optional.empty();
        }
        NotificationDelivery row = deliveryMapper.selectById(id);
        if (row == null) {
            return Optional.empty();
        }
        if (row.getAttemptCount() != null && row.getAttemptCount() > maxAttempts) {
            deliveryMapper.markPermanent(id, token, FailureCategory.ORPHAN_MAX_ATTEMPTS.name(),
                    FailureCategory.ORPHAN_MAX_ATTEMPTS.name(), now);
            return Optional.empty();
        }
        if (row.getSendAttemptedAt() != null) {
            int unknown = row.getUnknownOutcomeCount() == null ? 0 : row.getUnknownOutcomeCount();
            if (unknown >= 1) {
                deliveryMapper.markPermanent(id, token, FailureCategory.UNKNOWN_OUTCOME.name(),
                        FailureCategory.UNKNOWN_OUTCOME.name(), now);
                NotificationLog.warn("unknown_outcome", row.getEventId(), row.getTenantId(),
                        row.getNotificationType(), row.getChannel(), "FAILED_PERMANENT",
                        FailureCategory.UNKNOWN_OUTCOME.name(), null, row.getRecipientUserId(),
                        row.getAttemptCount(), token, "max-resend-exhausted");
                return Optional.empty();
            }
            int promoted = deliveryMapper.promoteUnknownOnce(id, token, now);
            if (promoted == 0) {
                return Optional.empty();
            }
            NotificationLog.warn("unknown_outcome", row.getEventId(), row.getTenantId(),
                    row.getNotificationType(), row.getChannel(), "PROCESSING",
                    FailureCategory.UNKNOWN_OUTCOME.name(), null, row.getRecipientUserId(),
                    row.getAttemptCount(), token, "resend-once");
            row.setUnknownOutcomeCount(1);
            row.setSendAttemptedAt(null);
        } else if ("PROCESSING".equals(row.getStatus()) && row.getAttemptCount() != null && row.getAttemptCount() > 1) {
            NotificationLog.info("orphan_reclaimed", row.getEventId(), row.getTenantId(),
                    row.getNotificationType(), row.getChannel(), "PROCESSING",
                    null, null, row.getRecipientUserId(), row.getAttemptCount(), token);
        }
        return Optional.of(new Claimed<>(row, token));
    }

    @Transactional
    public Optional<Claimed<NotificationDigestEmail>> claimDigestEmail(Long id, LocalDateTime now,
                                                                       LocalDateTime leaseUntil, int maxAttempts) {
        String token = UUID.randomUUID().toString();
        int claimed = digestEmailMapper.claim(id, token, leaseUntil, now);
        if (claimed == 0) {
            return Optional.empty();
        }
        NotificationDigestEmail row = digestEmailMapper.selectById(id);
        if (row == null) {
            return Optional.empty();
        }
        if (row.getAttemptCount() != null && row.getAttemptCount() > maxAttempts) {
            digestEmailMapper.markPermanent(id, token, FailureCategory.ORPHAN_MAX_ATTEMPTS.name(),
                    FailureCategory.ORPHAN_MAX_ATTEMPTS.name(), now);
            deliveryMapper.markItemsByDigestEmailId(id, "FAILED_PERMANENT", now);
            return Optional.empty();
        }
        if (row.getSendAttemptedAt() != null) {
            int unknown = row.getUnknownOutcomeCount() == null ? 0 : row.getUnknownOutcomeCount();
            if (unknown >= 1) {
                digestEmailMapper.markPermanent(id, token, FailureCategory.UNKNOWN_OUTCOME.name(),
                        FailureCategory.UNKNOWN_OUTCOME.name(), now);
                deliveryMapper.markItemsByDigestEmailId(id, "FAILED_PERMANENT", now);
                return Optional.empty();
            }
            int promoted = digestEmailMapper.promoteUnknownOnce(id, token, now);
            if (promoted == 0) {
                return Optional.empty();
            }
            row.setUnknownOutcomeCount(1);
            row.setSendAttemptedAt(null);
        }
        return Optional.of(new Claimed<>(row, token));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markDeliverySendAttempted(Long id, String token, LocalDateTime now) {
        return deliveryMapper.markSendAttempted(id, token, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markDigestSendAttempted(Long id, String token, LocalDateTime now) {
        return digestEmailMapper.markSendAttempted(id, token, now);
    }

    public record Claimed<T>(T row, String token) {
    }
}
