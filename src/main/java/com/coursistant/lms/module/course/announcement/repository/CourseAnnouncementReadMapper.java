package com.coursistant.lms.module.course.announcement.repository;

import com.coursistant.lms.module.course.announcement.entity.CourseAnnouncementRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseAnnouncementReadMapper {

    int insertIgnore(CourseAnnouncementRead read);

    CourseAnnouncementRead selectByAnnouncementAndUser(@Param("announcementId") Integer announcementId,
                                                       @Param("userId") Integer userId);
}
