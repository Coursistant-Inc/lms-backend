package com.coursistant.lms.module.interaction.notification.repository;

import com.coursistant.lms.module.interaction.notification.entity.NotificationDigestEmail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationDigestEmailMapper {

    int insertCollecting(NotificationDigestEmail row);

    NotificationDigestEmail selectByKey(@Param("tenantId") Integer tenantId,
                                        @Param("recipientUserId") Integer recipientUserId,
                                        @Param("digestDate") LocalDate digestDate);

    NotificationDigestEmail selectById(@Param("id") Long id);

    int freezeCollected(@Param("id") Long id,
                        @Param("itemCount") int itemCount,
                        @Param("now") LocalDateTime now);

    int markSkippedIneligible(@Param("id") Long id, @Param("now") LocalDateTime now);

    List<Long> selectClaimBatch(@Param("now") LocalDateTime now,
                                @Param("batchSize") int batchSize,
                                @Param("tenantId") Integer tenantId,
                                @Param("digestDate") LocalDate digestDate);

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

    int cancelPendingForRecipient(@Param("recipientUserId") Integer recipientUserId,
                                  @Param("now") LocalDateTime now);
}
