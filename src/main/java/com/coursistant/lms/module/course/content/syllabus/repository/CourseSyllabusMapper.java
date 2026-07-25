package com.coursistant.lms.module.course.content.syllabus.repository;

import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseSyllabusMapper {
    int insert(CourseSyllabus syllabus);
    int updateVersions(CourseSyllabus syllabus);
    CourseSyllabus selectByCourseId(@Param("courseId") Integer courseId);
}
