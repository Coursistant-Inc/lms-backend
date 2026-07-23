package com.coursistant.lms.module.course.repository;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.coursistant.lms.module.course.entity.Course;
import com.coursistant.lms.module.course.dto.CourseDetailsDTO;
import com.coursistant.lms.module.chat.entity.Query;

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

    List<Course> selectByUserIdFromTeach(@Param("userId") Integer userId);

    List<Course> selectByUserIdFromLearn(@Param("userId") Integer userId);

    List<CourseDetailsDTO> selectCourseDetailsByUserId(Integer userId, List<Course> courseList);

    void updateLastSelectedCourse(Integer userId, Integer courseId);


}
