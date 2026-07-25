package com.coursistant.lms.module.course.content.syllabus.repository;

import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabusVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseSyllabusVersionMapper {
    int insert(CourseSyllabusVersion version);
    CourseSyllabusVersion selectById(@Param("id") Integer id);
}
