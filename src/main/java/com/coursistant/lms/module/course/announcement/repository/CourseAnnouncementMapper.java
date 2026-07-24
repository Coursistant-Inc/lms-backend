package com.coursistant.lms.module.course.announcement.repository;

import com.coursistant.lms.module.course.announcement.dto.AnnouncementSummaryResponse;
import com.coursistant.lms.module.course.announcement.entity.CourseAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseAnnouncementMapper {

    int insert(CourseAnnouncement announcement);

    int updateById(CourseAnnouncement announcement);

    int deleteById(@Param("id") Integer id);

    CourseAnnouncement selectById(@Param("id") Integer id);

    List<AnnouncementSummaryResponse> selectSummariesByCourseId(@Param("courseId") Integer courseId,
                                                                @Param("userId") Integer userId,
                                                                @Param("limit") int limit);

    List<AnnouncementSummaryResponse> selectRecentForUser(@Param("userId") Integer userId,
                                                          @Param("limit") int limit);
}
