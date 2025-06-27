package com.coursistant.lms.mapper.file;

import com.coursistant.lms.entity.DiskFiles;
import com.coursistant.lms.entity.Folder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * FolderMapper
 * 对应 Folder 表（Lecture 文件夹）的 Mapper 接口
 */
public interface FolderMapper {

    /**
     * 插入新记录
     */
    int insert(Folder folder);

    /**
     * 根据主键删除
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 根据主键更新
     */
    int updateById(Folder folder);

    /**
     * 根据主键查询
     */
    Folder selectById(@Param("id") Integer id);

    /**
     * 多条件查询所有记录
     */
    List<Folder> selectAll(Folder folder);

    /**
     * 根据课程 ID 查询该课程下的所有 Folder（Lecture）
     */
    List<Folder> selectByCourseId(@Param("courseId") Integer courseId);

}
