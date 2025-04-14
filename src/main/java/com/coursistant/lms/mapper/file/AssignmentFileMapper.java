package com.coursistant.lms.mapper.file;

import com.coursistant.lms.entity.AssignmentFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 操作 AssignmentFile 相关数据接口
 * Data access interface for AssignmentFile-related operations
 */
@Mapper
public interface AssignmentFileMapper {

    /**
     * 新增 AssignmentFileFile
     * Insert a new AssignmentFileFile
     */
    int insert(AssignmentFile assignmentFile);

    /**
     * 根据 ID 删除 AssignmentFile
     * Delete an AssignmentFile by ID
     */
    int deleteById(Integer id);

    /**
     * 根据 ID 更新 AssignmentFile
     * Update an AssignmentFile by ID
     */
    int updateById(AssignmentFile assignmentFile);

    /**
     * 根据 ID 查询 AssignmentFile
     * Query an AssignmentFile by ID
     */
    AssignmentFile selectById(Integer id);

    /**
     * 查询所有 AssignmentFile（这里假设不带参数筛选）
     * Query all AssignmentFiles (assuming no parameter filtering)
     */
    List<AssignmentFile> selectAll(AssignmentFile assignmentFile);

    /**
     * 根据 user_id 查询 AssignmentFile
     * Query AssignmentFiles by assignment_id
     */
    @Select("SELECT * FROM AssignmentFile WHERE assignment_id = #{assignmentId}")
    List<AssignmentFile> selectByAssignmentId(Integer assignmentId);
}
