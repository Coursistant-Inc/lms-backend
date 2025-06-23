package com.coursistant.lms.mapper.assignment;

import com.coursistant.lms.entity.Assignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 操作 Assignment 相关数据接口
 * Data access interface for Assignment-related operations
 */
@Mapper
public interface AssignmentMapper {

    /**
     * 新增 Assignment
     * Insert a new Assignment
     */
    int insert(Assignment assignment);

    /**
     * 根据 ID 删除 Assignment
     * Delete an Assignment by ID
     */
    int deleteById(Integer id);

    /**
     * 根据 ID 更新 Assignment
     * Update an Assignment by ID
     */
    int updateById(Assignment assignment);

    /**
     * 根据 ID 增加 Assignment Submission Number
     * Increment the submission number by 1 in an Assignment by ID
     */
    int incrementSubNumById(Assignment assignment);

    /**
     * 根据 ID 查询 Assignment
     * Query an Assignment by ID
     */
    Assignment selectById(Integer id);

    /**
     * 查询所有 Assignment（这里假设不带参数筛选）
     * Query all Assignments (assuming no parameter filtering)
     */
    List<Assignment> selectAll(Assignment assignment);

    /**
     * 根据 user_id 查询 Assignment
     * Query Assignments by user_id
     */
    @Select("SELECT * FROM Assignment WHERE user_id = #{userId}")
    List<Assignment> selectByUserId(Integer userId);

    /**
     * 根据 course_id 查询 Assignment
     * Query Assignments by course_id
     */
    @Select("SELECT * FROM Assignment WHERE course_id = #{courseId}")
    List<Assignment> selectByCourseId(Integer courseId);

    /**
     * 查询某学生在指定时间范围内所有课程的作业（用于日历展示）
     */
    List<Assignment> selectAssignmentsByUserAndTimeRange(@Param("userId") Integer userId,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);








}
