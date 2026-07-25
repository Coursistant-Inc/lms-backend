package com.coursistant.lms.module.course.course.repository;

import com.coursistant.lms.module.course.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseMapper {

    int insert(Course course);

    Course selectById(Integer id);

    int updateById(Course course);

    int deleteById(Integer id);

    int archiveById(@Param("id") Integer id, @Param("archivedAt") LocalDateTime archivedAt);

    int unarchiveById(@Param("id") Integer id);

    long countForBrowse(@Param("q") String q,
                        @Param("state") String state,
                        @Param("instructorUserId") Integer instructorUserId);

    List<Course> selectForBrowse(@Param("q") String q,
                                 @Param("state") String state,
                                 @Param("instructorUserId") Integer instructorUserId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);
}
