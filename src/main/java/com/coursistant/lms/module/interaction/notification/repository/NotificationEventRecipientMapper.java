package com.coursistant.lms.module.interaction.notification.repository;

import com.coursistant.lms.module.interaction.notification.entity.NotificationEventRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationEventRecipientMapper {

    int insertChunk(@Param("rows") List<NotificationEventRecipient> rows);

    List<Integer> selectRecipientIds(@Param("outboxId") Long outboxId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    int countByOutboxId(@Param("outboxId") Long outboxId);
}
