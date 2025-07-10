package com.coursistant.lms.mapper.assignment;

import com.coursistant.lms.entity.AssignmentSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 操作 AssignmentSubmission 相关数据接口
 * Data access interface for AssignmentSubmission-related operations
 */
@Mapper
public interface AssignmentSubmissionMapper {

    /**
     * 新增 AssignmentSubmission
     * Insert a new AssignmentSubmission
     */
    int insert(AssignmentSubmission assignmentSubmission);

    /**
     * 根据 ID 删除 AssignmentSubmission
     * Delete an AssignmentSubmission by ID
     */
    int deleteById(Integer id);

    /**
     * 根据 ID 更新 AssignmentSubmission
     * Update an AssignmentSubmission by ID
     */
    int updateById(AssignmentSubmission assignmentSubmission);

    /**
     * 根据 ID 查询 AssignmentSubmission
     * Query an AssignmentSubmission by ID
     */
    AssignmentSubmission selectById(Integer id);

    /**
     * 查询所有 AssignmentSubmission（这里假设不带参数筛选）
     * Query all Assignments (assuming no parameter filtering)
     */
    List<AssignmentSubmission> selectAll(AssignmentSubmission assignmentSubmission);

    /**
     * 根据 user_id 查询 AssignmentSubmission
     * Query Assignments by user_id
     */
    @Select("SELECT * FROM AssignmentSubmission WHERE user_id = #{userId}")
    List<AssignmentSubmission> selectByUserId(Integer userId);

    /**
     * 清除该学生该作业的所有 is_final = true 标记
     * Clear all is_final flags for a student's submissions on a specific assignment
     */
    void clearFinalFlag(@Param("assignmentId") int assignmentId,
                        @Param("studentId") int studentId);


    List<AssignmentSubmission> selectFinalGradedByAssignmentId(@Param("assignmentId") Integer assignmentId);


}
