package com.coursistant.lms.mapper.assignment;

import com.coursistant.lms.entity.Assignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
