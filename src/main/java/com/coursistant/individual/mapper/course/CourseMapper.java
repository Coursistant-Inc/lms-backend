package com.coursistant.individual.mapper.course;

import com.coursistant.individual.entity.Course;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作 course 相关数据接口
 * Data access interface for course-related operations
 */
public interface CourseMapper {

    /**
     * 新增
     * Insert a new course record
     */
    int insert(Course course);

    /**
     * 删除
     * Delete a course record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a course record by ID
     */
    int updateById(Course course);

    /**
     * 根据 ID 查询
     * Query a course record by ID
     */
    Course selectById(Integer id);

    /**
     * 查询所有
     * Query all course records
     */
    List<Course> selectAll(Course course);

    /**
     * 根据课程名称查询
     * Query a course by course name
     */
    @Select("select * from Course where coursename = #{coursename}")
    Course selectByCoursename(String coursename);
}
