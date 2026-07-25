package com.coursistant.lms.module.course.schedule.repository;

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
}
