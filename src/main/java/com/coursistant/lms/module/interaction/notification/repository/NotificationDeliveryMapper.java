package com.coursistant.lms.module.interaction.notification.repository;

import com.coursistant.lms.module.interaction.notification.dto.DigestRecipientKey;
import com.coursistant.lms.module.interaction.notification.entity.NotificationDelivery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationDeliveryMapper {

    int upsertChunk(@Param("rows") List<NotificationDelivery> rows);

    NotificationDelivery selectById(@Param("id") Long id);

    List<Long> selectClaimBatch(@Param("channel") String channel,
                                @Param("now") LocalDateTime now,
                                @Param("batchSize") int batchSize);

    int claim(@Param("id") Long id,
              @Param("claimToken") String claimToken,
              @Param("leaseUntil") LocalDateTime leaseUntil,
              @Param("now") LocalDateTime now);

    int promoteUnknownOnce(@Param("id") Long id,
                           @Param("claimToken") String claimToken,
                           @Param("now") LocalDateTime now);

    int markSendAttempted(@Param("id") Long id,
                          @Param("claimToken") String claimToken,
                          @Param("now") LocalDateTime now);

    int markSent(@Param("id") Long id,
                 @Param("claimToken") String claimToken,
                 @Param("providerMessageId") String providerMessageId,
                 @Param("now") LocalDateTime now);

    int markDryRun(@Param("id") Long id,
                   @Param("claimToken") String claimToken,
                   @Param("providerMessageId") String providerMessageId,
                   @Param("now") LocalDateTime now);

    int markRetry(@Param("id") Long id,
                  @Param("claimToken") String claimToken,
                  @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                  @Param("failureCategory") String failureCategory,
                  @Param("lastError") String lastError,
                  @Param("now") LocalDateTime now);

    int markPermanent(@Param("id") Long id,
                      @Param("claimToken") String claimToken,
                      @Param("failureCategory") String failureCategory,
                      @Param("lastError") String lastError,
                      @Param("now") LocalDateTime now);

    int markSkipped(@Param("id") Long id,
                    @Param("claimToken") String claimToken,
                    @Param("status") String status,
                    @Param("failureCategory") String failureCategory,
                    @Param("now") LocalDateTime now);

    int cancelPendingEmailsForRecipient(@Param("recipientUserId") Integer recipientUserId,
                                        @Param("now") LocalDateTime now);

    List<DigestRecipientKey> selectPendingDigestRecipients(@Param("digestDate") LocalDate digestDate,
                                                           @Param("tenantId") Integer tenantId);

    int attachDigestItems(@Param("digestEmailId") Long digestEmailId,
                          @Param("digestDate") LocalDate digestDate,
                          @Param("tenantId") Integer tenantId,
                          @Param("recipientUserId") Integer recipientUserId);

    int skipPendingDigestForRecipient(@Param("tenantId") Integer tenantId,
                                      @Param("recipientUserId") Integer recipientUserId,
                                      @Param("digestDate") LocalDate digestDate);

    int bumpUnattachedDigestDate(@Param("tenantId") Integer tenantId,
                                 @Param("recipientUserId") Integer recipientUserId,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate);

    List<NotificationDelivery> selectByDigestEmailId(@Param("digestEmailId") Long digestEmailId);

    int countByDigestEmailId(@Param("digestEmailId") Long digestEmailId);

    int markItemsByDigestEmailId(@Param("digestEmailId") Long digestEmailId,
                                 @Param("status") String status,
                                 @Param("now") LocalDateTime now);

    int requeueDelivery(@Param("id") Long id, @Param("now") LocalDateTime now);

    int requeueDryRunInRange(@Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             @Param("tenantId") Integer tenantId,
                             @Param("channel") String channel,
                             @Param("now") LocalDateTime now);

    int restoreItemsForDigestEmails(@Param("digestEmailIds") List<Long> digestEmailIds);
}
