package com.coursistant.lms.module.course.content.week.repository;

import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseWeekMapper {
    int insert(CourseWeek week);

    int updateById(CourseWeek week);

    int deleteById(@Param("id") Integer id);

    CourseWeek selectById(@Param("id") Integer id);

    List<CourseWeek> selectByCourseId(@Param("courseId") Integer courseId);

    Integer selectMaxOrderPosition(@Param("courseId") Integer courseId);

    int updateOrderPosition(@Param("id") Integer id, @Param("orderPosition") Integer orderPosition);

    int updateState(@Param("id") Integer id, @Param("state") String state);
}
