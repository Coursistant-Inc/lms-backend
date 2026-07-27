package com.coursistant.lms.module.course.event.repository;

import com.coursistant.lms.module.course.event.dto.UpcomingCourseActivityResponse;
import com.coursistant.lms.module.course.event.entity.CourseEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CourseEventMapper {
    int insert(CourseEvent event);
    int updateById(CourseEvent event);
    int deleteById(@Param("id") Integer id);
    CourseEvent selectById(@Param("id") Integer id);
    List<CourseEvent> selectByCourseId(@Param("courseId") Integer courseId);

    /**
     * Course events for the user's active enrollments with event_date in
     * {@code [fromDate, toDate]} (inclusive), shaped as dashboard activities.
     */
    List<UpcomingCourseActivityResponse> selectUpcomingActivitiesForUser(@Param("userId") Integer userId,
                                                                         @Param("fromDate") LocalDate fromDate,
                                                                         @Param("toDate") LocalDate toDate);
}
