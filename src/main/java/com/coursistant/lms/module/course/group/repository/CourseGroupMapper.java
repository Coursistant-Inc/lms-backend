package com.coursistant.lms.module.course.group.repository;

import com.coursistant.lms.module.course.group.entity.CourseGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseGroupMapper {

    int insert(CourseGroup group);

    int updateById(CourseGroup group);

    int deleteById(@Param("id") Integer id);

    CourseGroup selectById(@Param("id") Integer id);

    CourseGroup selectByIdForUpdate(@Param("id") Integer id);

    List<CourseGroup> selectByGroupSetId(@Param("groupSetId") Integer groupSetId);

    int countByGroupSetId(@Param("groupSetId") Integer groupSetId);
}
