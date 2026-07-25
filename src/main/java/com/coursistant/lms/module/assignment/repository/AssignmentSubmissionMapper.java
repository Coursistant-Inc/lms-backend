package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;


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
    @Select("SELECT * FROM assignment_submission WHERE user_id = #{userId}")
    List<AssignmentSubmission> selectByUserId(Integer userId);

    /**
     * 清除该学生该作业的所有 is_final = true 标记
     * Clear all is_final flags for a student's submissions on a specific assignment
     */
    void clearFinalFlag(@Param("assignmentId") int assignmentId,
                        @Param("studentId") int studentId);


    List<AssignmentSubmission> selectFinalGradedByAssignmentId(@Param("assignmentId") Integer assignmentId);


    /**
     * 查询指定 assignmentId + studentId 的最终提交
     * Query the final submission by assignmentId and studentId
     */
    AssignmentSubmission selectFinalByAssignmentIdAndUserId(@Param("assignmentId") Integer assignmentId,
                                                            @Param("studentId") Integer studentId);

    /**
     * 查询指定 assignmentId + groupId 的最终提交
     * Query the final submission by assignmentId and groupId
     */
    AssignmentSubmission selectFinalByAssignmentIdAndGroupId(@Param("assignmentId") Integer assignmentId,
                                                             @Param("groupId") Integer groupId);

    List<AssignmentSubmission> selectFinalByAssignmentIdsAndStudent(
            @Param("assignmentIds") List<Integer> assignmentIds,
            @Param("studentId") Integer studentId
    );



}
