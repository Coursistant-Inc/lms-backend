package com.coursistant.lms.module.interaction.notification.digest;

import com.coursistant.lms.module.interaction.notification.claim.NotificationClaimService;
import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.dto.DigestRecipientKey;
import com.coursistant.lms.module.interaction.notification.email.EmailMessage;
import com.coursistant.lms.module.interaction.notification.email.EmailSendResult;
import com.coursistant.lms.module.interaction.notification.email.NotificationContactLookup;
import com.coursistant.lms.module.interaction.notification.email.NotificationEmailSender;
import com.coursistant.lms.module.interaction.notification.email.NotificationEmailTemplateFactory;
import com.coursistant.lms.module.interaction.notification.email.RenderedEmail;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDigestEmail;
import com.coursistant.lms.module.interaction.notification.enums.DigestEmailStatus;
import com.coursistant.lms.module.interaction.notification.enums.FailureCategory;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDigestEmailMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.interaction.notification.support.NotificationJson;
import com.coursistant.lms.module.interaction.notification.support.NotificationLog;
import com.coursistant.lms.module.user.account.entity.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DailyDigestService {

    @Resource
    private NotificationDeliveryMapper deliveryMapper;

    @Resource
    private NotificationDigestEmailMapper digestEmailMapper;

    @Resource
    private NotificationContactLookup contactLookup;

    @Resource
    private NotificationClaimService claimService;

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

    public void run(LocalDate digestDate, Integer tenantId) {
        if (digestDate == null) {
            digestDate = LocalDate.now();
        }
        List<DigestRecipientKey> keys = deliveryMapper.selectPendingDigestRecipients(digestDate, tenantId);
        if (keys != null) {
            for (DigestRecipientKey key : keys) {
                try {
                    collectOne(key.getTenantId(), key.getRecipientUserId(), digestDate);
                } catch (Exception e) {
                    NotificationLog.warn("digest_phase_a_failed", null, key.getTenantId(), null,
                            "DAILY_DIGEST", null, null, null, key.getRecipientUserId(), null, null,
                            e.getMessage());
                }
            }
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        List<Long> ids = digestEmailMapper.selectClaimBatch(now, notificationProperties.getDigest().getBatchSize());
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            try {
                sendOne(id);
            } catch (Exception e) {
                NotificationLog.warn("digest_phase_b_failed", null, null, null, "DAILY_DIGEST",
                        null, null, null, null, null, null, e.getMessage());
            }
        }
    }

    @Transactional
    public void collectOne(Integer tenantId, Integer recipientUserId, LocalDate digestDate) {
        LocalDateTime now = notificationTimeSupport.nowUtc();
        User user = contactLookup.load(List.of(recipientUserId)).get(recipientUserId);
        if (!contactLookup.emailEnabled(user)) {
            deliveryMapper.skipPendingDigestForRecipient(tenantId, recipientUserId, digestDate);
            return;
        }
        NotificationDigestEmail insert = new NotificationDigestEmail();
        insert.setTenantId(tenantId);
        insert.setRecipientUserId(recipientUserId);
        insert.setDigestDate(digestDate);
        insert.setNextAttemptAt(now);
        insert.setCreatedAt(now);
        insert.setUpdatedAt(now);
        digestEmailMapper.insertCollecting(insert);
        NotificationDigestEmail row = digestEmailMapper.selectByKey(tenantId, recipientUserId, digestDate);
        if (row == null) {
            return;
        }
        String status = row.getStatus();
        if (DigestEmailStatus.COLLECTING.name().equals(status)) {
            deliveryMapper.attachDigestItems(row.getId(), digestDate, tenantId, recipientUserId);
            int count = deliveryMapper.countByDigestEmailId(row.getId());
            if (count == 0) {
                digestEmailMapper.markSkippedIneligible(row.getId(), now);
            } else {
                digestEmailMapper.freezeCollected(row.getId(), count, now);
            }
            return;
        }
        deliveryMapper.bumpUnattachedDigestDate(tenantId, recipientUserId, digestDate, digestDate.plusDays(1));
    }

    public void sendOne(Long digestEmailId) {
        LocalDateTime now = notificationTimeSupport.nowUtc();
        LocalDateTime leaseUntil = now.plusSeconds(notificationProperties.getDigest().getLeaseSeconds());
        var claimed = claimService.claimDigestEmail(digestEmailId, now, leaseUntil,
                notificationProperties.getDigest().getMaxAttempts());
        if (claimed.isEmpty()) {
            return;
        }
        NotificationDigestEmail row = claimed.get().row();
        String token = claimed.get().token();
        User user = contactLookup.load(List.of(row.getRecipientUserId())).get(row.getRecipientUserId());
        if (!contactLookup.emailEnabled(user)) {
            digestEmailMapper.markSkipped(digestEmailId, token, "SKIPPED_PREFERENCE",
                    FailureCategory.PERMANENT_PREFERENCE.name(), now);
            deliveryMapper.markItemsByDigestEmailId(digestEmailId, "SKIPPED_PREFERENCE", now);
            return;
        }
        List<NotificationDelivery> items = deliveryMapper.selectByDigestEmailId(digestEmailId);
        if (items == null || items.isEmpty()) {
            digestEmailMapper.markSkipped(digestEmailId, token, "SKIPPED_INELIGIBLE",
                    FailureCategory.PERMANENT_MISSING_TEMPLATE.name(), now);
            return;
        }
        RenderedEmail rendered = templateFactory.renderDigest(row.getDigestDate(), groupItems(items));
        if (digestEmailMapper.markSendAttempted(digestEmailId, token, now) == 0) {
            NotificationLog.warn("stale_claim", null, row.getTenantId(), null, "DAILY_DIGEST",
                    "PROCESSING", null, null, row.getRecipientUserId(), row.getAttemptCount(), token,
                    "skip-smtp");
            return;
        }
        if (!contactLookup.hasUsableEmail(user)) {
            digestEmailMapper.markPermanent(digestEmailId, token, FailureCategory.PERMANENT_NO_EMAIL.name(),
                    "no-email", now);
            deliveryMapper.markItemsByDigestEmailId(digestEmailId, "FAILED_PERMANENT", now);
            return;
        }
        EmailSendResult result = emailSender.send(new EmailMessage(
                row.getRecipientUserId(), user.getEmail(), rendered.subject(), rendered.textBody()));
        switch (result.status()) {
            case SENT -> {
                if (digestEmailMapper.markSent(digestEmailId, token, result.providerMessageId(), now) == 0) {
                    NotificationLog.warn("stale_claim", null, row.getTenantId(), null, "DAILY_DIGEST",
                            "SENT", null, null, row.getRecipientUserId(), row.getAttemptCount(), token, "markSent");
                    return;
                }
                deliveryMapper.markItemsByDigestEmailId(digestEmailId, "SENT", now);
            }
            case DRY_RUN -> {
                if (digestEmailMapper.markDryRun(digestEmailId, token, result.providerMessageId(), now) == 0) {
                    return;
                }
                deliveryMapper.markItemsByDigestEmailId(digestEmailId, "DRY_RUN", now);
            }
            case RETRYABLE_FAILURE -> digestEmailMapper.markRetry(digestEmailId, token,
                    now.plusSeconds(Math.max(2, (long) Math.min(3600,
                            Math.pow(notificationProperties.getEmail().getBackoffBaseSeconds(),
                                    row.getAttemptCount() == null ? 1 : row.getAttemptCount())))),
                    result.failureCategory() == null ? FailureCategory.RETRYABLE_NETWORK.name()
                            : result.failureCategory().name(),
                    result.errorMessage(), now);
            case PERMANENT_FAILURE -> {
                digestEmailMapper.markPermanent(digestEmailId, token,
                        result.failureCategory() == null ? FailureCategory.PERMANENT_NO_EMAIL.name()
                                : result.failureCategory().name(),
                        result.errorMessage(), now);
                deliveryMapper.markItemsByDigestEmailId(digestEmailId, "FAILED_PERMANENT", now);
            }
        }
    }

    List<NotificationEmailTemplateFactory.DigestCourseGroup> groupItems(List<NotificationDelivery> items) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        Map<String, String> titles = new LinkedHashMap<>();
        for (NotificationDelivery item : items) {
            Map<String, String> vars = notificationJson.readVars(item.getTemplateVarsJson());
            String code = vars.getOrDefault("courseCode", "COURSE");
            String title = vars.getOrDefault("courseTitle", code);
            titles.putIfAbsent(code, title);
            grouped.computeIfAbsent(code, k -> new ArrayList<>()).add(item.getMessage());
        }
        List<NotificationEmailTemplateFactory.DigestCourseGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            groups.add(new NotificationEmailTemplateFactory.DigestCourseGroup(
                    entry.getKey(), titles.get(entry.getKey()), entry.getValue()));
        }
        return groups;
    }
}
