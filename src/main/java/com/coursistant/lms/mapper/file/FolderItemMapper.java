package com.coursistant.lms.mapper.file;


import com.coursistant.lms.entity.DiskFiles;
import com.coursistant.lms.entity.FolderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * FolderItemMapper
 * 对应 FolderItem 表（文件夹资源项）的 Mapper 接口
 */

public interface FolderItemMapper {

    /**
     * 插入新记录
     */
    int insert(FolderItem folderItem);

    /**
     * 根据主键删除
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 根据主键更新
     */
    int updateById(FolderItem folderItem);

    /**
     * 根据主键查询
     */
    FolderItem selectById(@Param("id") Integer id);

    /**
     * 多条件查询所有记录
     */
    List<FolderItem> selectAll(FolderItem folderItem);

    /**
     * 查询某个文件夹下的所有资源项
     */
    List<FolderItem> selectByFolderId(@Param("folderId") Integer folderId);

    /**
     * 根据文件夹 ID 删除资源项
     * Delete folder items by folder ID
     */
    int deleteByFolderId(@Param("folderId") Integer folderId);

    List<FolderItem> selectCourseInfo(@Param("courseId") Integer courseId);

    List<DiskFiles> selectFolderFilesByCourseId(@Param("courseId") Integer courseId);

}
