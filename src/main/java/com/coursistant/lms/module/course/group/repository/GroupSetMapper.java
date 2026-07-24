package com.coursistant.lms.module.course.group.repository;

import com.coursistant.lms.module.course.group.entity.GroupSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupSetMapper {

    int insert(GroupSet groupSet);

    int updateById(GroupSet groupSet);

    int updateAll(GroupSet groupSet);

    int deleteById(@Param("id") Integer id);

    GroupSet selectById(@Param("id") Integer id);

    List<GroupSet> selectByCourseId(@Param("courseId") Integer courseId);
}
