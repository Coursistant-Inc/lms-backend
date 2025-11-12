package com.coursistant.lms.mapper.file;


import com.coursistant.lms.entity.AssignmentItem;
import com.coursistant.lms.entity.DiskFiles;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AssignmentItemMapper
 * 对应 AssignmentItem 表（文件夹资源项）的 Mapper 接口
 */

public interface AssignmentItemMapper {

    /**
     * 插入新记录
     */
    int insert(AssignmentItem assignmentItem);

    /**
     * 根据主键删除
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 根据主键更新
     */
    int updateById(AssignmentItem assignmentItem);

    /**
     * 根据主键查询
     */
    AssignmentItem selectById(@Param("id") Integer id);

    /**
     * 多条件查询所有记录
     */
    List<AssignmentItem> selectAll(AssignmentItem assignmentItem);

    /**
     * 查询某个文件夹下的所有资源项
     */
    List<AssignmentItem> selectByAssignmentId(@Param("assignmentId") Integer assignmentId);

    /**
     * 根据文件夹 ID 删除资源项
     * Delete assignment items by assignment ID
     */
    int deleteByAssignmentId(@Param("assignmentId") Integer assignmentId);

    List<AssignmentItem> selectCourseInfo(@Param("courseId") Integer courseId);

    List<DiskFiles> selectAssignmentFilesByCourseId(@Param("courseId") Integer courseId);
}
