package com.coursistant.lms.module.file.repository;

import com.coursistant.lms.module.file.entity.DiskFiles;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作 diskFiles 相关数据接口
 * Data access interface for diskFiles-related operations
 */
public interface DiskFilesMapper {

    /**
     * 新增
     * Insert a new DiskFiles record
     */
    int insert(DiskFiles diskFiles);

    /**
     * 删除
     * Delete a DiskFiles record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a DiskFiles record by ID
     */
    int updateById(DiskFiles diskFiles);

    /**
     * 根据 ID 查询
     * Query a DiskFiles record by ID
     */
    DiskFiles selectById(Integer id);

    /**
     * 查询所有
     * Query all DiskFiles records
     */
    List<DiskFiles> selectAll(DiskFiles diskFiles);

    /**
     * 根据课程名称查询
     * Query DiskFiles records by course name
     */
    @Select("select * from diskfiles where course_Id = #{courseId}")
    List<DiskFiles> selectByCourseName(Integer courseId);

    /**
     * 根据路径查询
     * Query a DiskFiles record by path
     */
    @Select("select * from diskfiles where path = #{path}")
    DiskFiles selectByPath(String path);




}
