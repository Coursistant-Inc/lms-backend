package com.coursistant.lms.mapper.file;

import com.coursistant.lms.entity.SubmissionFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 操作 SubmissionFile 相关数据接口
 * Data access interface for SubmissionFile-related operations
 */
@Mapper
public interface SubmissionFileMapper {

    /**
     * 新增 SubmissionFileFile
     * Insert a new SubmissionFileFile
     */
    int insert(SubmissionFile submissionFile);

    /**
     * 根据 ID 删除 SubmissionFile
     * Delete an SubmissionFile by ID
     */
    int deleteById(Integer id);

    /**
     * 根据 ID 更新 SubmissionFile
     * Update an SubmissionFile by ID
     */
    int updateById(SubmissionFile submissionFile);

    /**
     * 根据 ID 查询 SubmissionFile
     * Query an SubmissionFile by ID
     */
    SubmissionFile selectById(Integer id);

    /**
     * 查询所有 SubmissionFile（这里假设不带参数筛选）
     * Query all SubmissionFiles (assuming no parameter filtering)
     */
    List<SubmissionFile> selectAll(SubmissionFile submissionFile);

    /**
     * 根据 user_id 查询 SubmissionFile
     * Query SubmissionFiles by assignment_id
     */
    @Select("SELECT * FROM SubmissionFile WHERE submission_id = #{submissionId}")
    List<SubmissionFile> selectBySubmissionId(Integer submissionId);
}
