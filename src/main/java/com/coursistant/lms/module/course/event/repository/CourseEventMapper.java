package com.coursistant.lms.module.course.event.repository;

import com.coursistant.lms.module.course.event.entity.CourseEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseEventMapper {
    int insert(CourseEvent event);
    int updateById(CourseEvent event);
    int deleteById(@Param("id") Integer id);
    CourseEvent selectById(@Param("id") Integer id);
    List<CourseEvent> selectByCourseId(@Param("courseId") Integer courseId);
}
