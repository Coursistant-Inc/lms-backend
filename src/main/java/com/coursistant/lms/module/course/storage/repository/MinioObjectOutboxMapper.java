package com.coursistant.lms.module.course.storage.repository;

import com.coursistant.lms.module.course.storage.entity.MinioObjectOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MinioObjectOutboxMapper {
    int insert(MinioObjectOutbox row);

    List<MinioObjectOutbox> selectClaimBatch(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int claim(@Param("id") Long id,
              @Param("leaseUntil") LocalDateTime leaseUntil,
              @Param("now") LocalDateTime now);

    int markDone(@Param("id") Long id);

    int markRetry(@Param("id") Long id,
                  @Param("retryCount") int retryCount,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt,
                  @Param("lastError") String lastError);

    int markDead(@Param("id") Long id, @Param("lastError") String lastError);
}
