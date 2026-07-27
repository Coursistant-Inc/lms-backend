package com.coursistant.lms.module.course.schedule.repository;

import com.coursistant.lms.module.course.schedule.dto.SessionWithCourseCode;
import com.coursistant.lms.module.course.schedule.entity.CourseSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseSessionMapper {

    int insert(CourseSession session);

    int updateById(CourseSession session);

    int deleteById(@Param("id") Integer id);

    CourseSession selectById(@Param("id") Integer id);

    List<CourseSession> selectByCourseId(@Param("courseId") Integer courseId);

    /** Sessions for all courses where the user has an active enrollment. */
    List<SessionWithCourseCode> selectByUserActiveEnrollments(@Param("userId") Integer userId);
}
