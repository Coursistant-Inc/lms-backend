package com.coursistant.lms.module.interaction.notification.repository;

import com.coursistant.lms.module.interaction.notification.dto.NotificationSubjectRef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationAvailabilityMapper {

    List<NotificationSubjectRef> selectAnnouncements(@Param("ids") List<Integer> ids);

    List<NotificationSubjectRef> selectAssignments(@Param("ids") List<Integer> ids);

    List<NotificationSubjectRef> selectSubmissions(@Param("ids") List<Integer> ids);

    List<NotificationSubjectRef> selectQuizzes(@Param("ids") List<Integer> ids);

    List<NotificationSubjectRef> selectWeeks(@Param("ids") List<Integer> ids);

    List<NotificationSubjectRef> selectCourseEvents(@Param("ids") List<Integer> ids);

    List<NotificationSubjectRef> selectGroupSets(@Param("ids") List<Integer> ids);

    List<Integer> selectGroupIdsForUser(@Param("userId") Integer userId);
}
