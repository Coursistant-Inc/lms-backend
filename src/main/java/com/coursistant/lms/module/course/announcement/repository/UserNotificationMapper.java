package com.coursistant.lms.module.course.announcement.repository;

import com.coursistant.lms.module.course.announcement.dto.NotificationResponse;
import com.coursistant.lms.module.course.announcement.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserNotificationMapper {

    int insertIgnore(UserNotification notification);

    List<NotificationResponse> selectByRecipient(@Param("recipientUserId") Integer recipientUserId,
                                                 @Param("limit") int limit);

    UserNotification selectById(@Param("id") Integer id);

    int markRead(@Param("id") Integer id,
                 @Param("recipientUserId") Integer recipientUserId,
                 @Param("readAt") LocalDateTime readAt);

    int countByRecipientAndEventAndRef(@Param("recipientUserId") Integer recipientUserId,
                                       @Param("eventType") String eventType,
                                       @Param("refId") Integer refId);
}
