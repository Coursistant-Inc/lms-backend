package com.coursistant.lms.module.course.course.repository;

import com.coursistant.lms.module.course.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CourseMapper {

    int insert(Course course);

    Course selectById(Integer id);

    int updateById(Course course);

    int deleteById(Integer id);

    int archiveById(@Param("id") Integer id, @Param("archivedAt") LocalDateTime archivedAt);
}
