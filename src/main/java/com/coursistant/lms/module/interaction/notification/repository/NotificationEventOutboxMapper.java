package com.coursistant.lms.module.interaction.notification.repository;

import com.coursistant.lms.module.interaction.notification.entity.NotificationEventOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationEventOutboxMapper {

    int insertIgnoreDuplicate(NotificationEventOutbox row);

    NotificationEventOutbox selectByDedupeKey(@Param("tenantId") Integer tenantId,
                                              @Param("notificationType") String notificationType,
                                              @Param("subjectType") String subjectType,
                                              @Param("subjectId") Integer subjectId,
                                              @Param("eventKey") String eventKey);

    NotificationEventOutbox selectById(@Param("id") Long id);

    List<Long> selectClaimBatch(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);

    int claim(@Param("id") Long id,
              @Param("claimToken") String claimToken,
              @Param("leaseUntil") LocalDateTime leaseUntil,
              @Param("now") LocalDateTime now);

    int markDone(@Param("id") Long id, @Param("claimToken") String claimToken, @Param("now") LocalDateTime now);

    int markRetryable(@Param("id") Long id,
                      @Param("claimToken") String claimToken,
                      @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                      @Param("lastError") String lastError,
                      @Param("now") LocalDateTime now);

    int markPermanent(@Param("id") Long id,
                      @Param("claimToken") String claimToken,
                      @Param("lastError") String lastError,
                      @Param("now") LocalDateTime now);
}
