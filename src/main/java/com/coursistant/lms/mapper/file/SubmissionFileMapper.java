package com.coursistant.lms.mapper.file;

import com.coursistant.lms.entity.SubmissionFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 操作 SubmissionFile 相关数据接口
 * Data access interface for SubmissionFile-related operations
 */
@Mapper
public interface SubmissionFileMapper {

    /**
     * 插入新记录
     * Insert a new SubmissionFile
     */
    int insert(SubmissionFile submissionFile);

    /**
     * 根据主键删除
     * Delete a SubmissionFile by ID
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 根据主键更新
     * Update a SubmissionFile by ID
     */
    int updateById(SubmissionFile submissionFile);

    /**
     * 根据主键查询
     * Query a SubmissionFile by ID
     */
    SubmissionFile selectById(@Param("id") Integer id);

    /**
     * 多条件查询所有记录
     * Query all SubmissionFiles with multiple conditions
     */
    List<SubmissionFile> selectAll(SubmissionFile submissionFile);

    /**
     * 查询某个 submission 下的所有文件
     * Query all files under a submission
     */
    List<SubmissionFile> selectBySubmissionId(@Param("submissionId") Integer submissionId);

    /**
     * 根据 submission ID 删除文件
     * Delete submission files by submission ID
     */
    int deleteBySubmissionId(@Param("submissionId") Integer submissionId);

    // 查询某个 submission 的所有文件（与 XML 的 selectSubmissionInfo 对应）
    List<SubmissionFile> selectSubmissionInfo(@Param("submissionId") Integer submissionId);


    /**
     * 查询指定 submissionId 且 type='file' 的文件
     * Query a file with given submissionId and type='file'
     */
    List<SubmissionFile> selectFileBySubmissionId(@Param("submissionId") Integer submissionId);



}
