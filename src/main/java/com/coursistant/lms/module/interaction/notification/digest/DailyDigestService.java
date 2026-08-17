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
import com.coursistant.lms.module.interaction.notification.enums.DeliveryStatus;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Resource
    private PlatformTransactionManager transactionManager;

    public void run(LocalDate digestDate, Integer tenantId) {
        if (digestDate == null) {
            throw new IllegalArgumentException("digestDate is required");
        }
        List<DigestRecipientKey> keys = deliveryMapper.selectPendingDigestRecipients(digestDate, tenantId);
        if (keys != null) {
            for (DigestRecipientKey key : keys) {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                            collectOne(key.getTenantId(), key.getRecipientUserId(), digestDate));
                } catch (Exception e) {
                    NotificationLog.warn("digest_phase_a_failed", null, key.getTenantId(), null,
                            "DAILY_DIGEST", null, null, null, key.getRecipientUserId(), null, null,
                            e.getMessage());
                }
            }
        }
        if (!notificationProperties.getEmail().isEnabled()) {
            return;
        }
        LocalDateTime now = notificationTimeSupport.nowUtc();
        List<Long> ids = digestEmailMapper.selectClaimBatch(
                now, notificationProperties.getDigest().getBatchSize(), tenantId, digestDate);
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

    void collectOne(Integer tenantId, Integer recipientUserId, LocalDate digestDate) {
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
        if (!notificationProperties.getEmail().isEnabled()) {
            return;
        }
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
        now = notificationTimeSupport.nowUtc();
        if (!contactLookup.emailEnabled(user)) {
            if (!claimService.completeDigestTerminal(digestEmailId, token, new NotificationClaimService.DigestTerminal(
                    DigestEmailStatus.SKIPPED_PREFERENCE.name(), DeliveryStatus.SKIPPED_PREFERENCE.name(),
                    FailureCategory.PERMANENT_PREFERENCE.name(), null, null), now)) {
                stale(row, token, DigestEmailStatus.SKIPPED_PREFERENCE.name(), "preference");
            }
            return;
        }
        List<NotificationDelivery> items = deliveryMapper.selectByDigestEmailId(digestEmailId);
        if (items == null || items.isEmpty()) {
            if (!claimService.completeDigestTerminal(digestEmailId, token, new NotificationClaimService.DigestTerminal(
                    DigestEmailStatus.SKIPPED_INELIGIBLE.name(), null,
                    FailureCategory.PERMANENT_MISSING_TEMPLATE.name(), null, null), now)) {
                stale(row, token, DigestEmailStatus.SKIPPED_INELIGIBLE.name(), "empty-items");
            }
            return;
        }
        if (!contactLookup.hasUsableEmail(user) || !contactLookup.accountActive(user)) {
            if (!claimService.completeDigestTerminal(digestEmailId, token, new NotificationClaimService.DigestTerminal(
                    DigestEmailStatus.FAILED_PERMANENT.name(), DeliveryStatus.FAILED_PERMANENT.name(),
                    FailureCategory.PERMANENT_NO_EMAIL.name(), "no-email", null), now)) {
                stale(row, token, DigestEmailStatus.FAILED_PERMANENT.name(), "no-email");
            }
            return;
        }
        RenderedEmail rendered = templateFactory.renderDigest(row.getDigestDate(), groupItems(items));
        now = notificationTimeSupport.nowUtc();
        leaseUntil = now.plusSeconds(notificationProperties.getDigest().getLeaseSeconds());
        if (claimService.markDigestSendAttempted(digestEmailId, token, now, leaseUntil) == 0) {
            stale(row, token, DigestEmailStatus.PROCESSING.name(), "skip-smtp");
            return;
        }
        EmailSendResult result = emailSender.send(new EmailMessage(
                row.getRecipientUserId(), user.getEmail(), rendered.subject(), rendered.textBody()));
        LocalDateTime resultNow = notificationTimeSupport.nowUtc();
        switch (result.status()) {
            case SENT -> {
                if (!claimService.completeDigestTerminal(digestEmailId, token,
                        new NotificationClaimService.DigestTerminal(
                                DigestEmailStatus.SENT.name(), DeliveryStatus.SENT.name(),
                                null, null, result.providerMessageId()), resultNow)) {
                    stale(row, token, DigestEmailStatus.SENT.name(), "markSent");
                }
            }
            case DRY_RUN -> {
                if (!claimService.completeDigestTerminal(digestEmailId, token,
                        new NotificationClaimService.DigestTerminal(
                                DigestEmailStatus.DRY_RUN.name(), DeliveryStatus.DRY_RUN.name(),
                                null, null, result.providerMessageId()), resultNow)) {
                    stale(row, token, DigestEmailStatus.DRY_RUN.name(), "markDryRun");
                }
            }
            case RETRYABLE_FAILURE -> digestEmailMapper.markRetry(digestEmailId, token,
                    resultNow.plusSeconds(Math.max(2, (long) Math.min(3600,
                            Math.pow(notificationProperties.getEmail().getBackoffBaseSeconds(),
                                    row.getAttemptCount() == null ? 1 : row.getAttemptCount())))),
                    result.failureCategory() == null ? FailureCategory.RETRYABLE_NETWORK.name()
                            : result.failureCategory().name(),
                    NotificationLog.truncateLastError(result.errorMessage()), resultNow);
            case PERMANENT_FAILURE -> {
                if (!claimService.completeDigestTerminal(digestEmailId, token,
                        new NotificationClaimService.DigestTerminal(
                                DigestEmailStatus.FAILED_PERMANENT.name(),
                                DeliveryStatus.FAILED_PERMANENT.name(),
                                result.failureCategory() == null ? FailureCategory.PERMANENT_NO_EMAIL.name()
                                        : result.failureCategory().name(),
                                NotificationLog.truncateLastError(result.errorMessage()), null), resultNow)) {
                    stale(row, token, DigestEmailStatus.FAILED_PERMANENT.name(), "provider");
                }
            }
            case UNKNOWN_OUTCOME -> NotificationLog.warn("unknown_outcome", null, row.getTenantId(), null,
                    "DAILY_DIGEST", "PROCESSING", FailureCategory.UNKNOWN_OUTCOME.name(), null,
                    row.getRecipientUserId(), row.getAttemptCount(), token,
                    result.errorMessage() == null ? "smtp-unknown-outcome" : result.errorMessage());
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

    private void stale(NotificationDigestEmail row, String token, String status, String detail) {
        NotificationLog.warn("stale_claim", null, row.getTenantId(), null, "DAILY_DIGEST",
                status, null, null, row.getRecipientUserId(), row.getAttemptCount(), token, detail);
    }
}
