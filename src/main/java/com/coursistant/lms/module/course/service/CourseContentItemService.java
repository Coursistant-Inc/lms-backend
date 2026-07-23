package com.coursistant.lms.module.course.service;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.module.file.entity.DiskFiles;
import com.coursistant.lms.module.file.entity.FolderItem;
import com.coursistant.lms.module.file.repository.FolderItemMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.List;


@Service
public class CourseContentItemService {

    @Resource
    private FolderItemMapper folderItemMapper;

    /**
     * 新增
     * Add new folder item
     */
    public Integer add(FolderItem folderItem) {
        if (ObjectUtil.isNull(folderItem.getOrderIndex())){
            List<FolderItem> existing = null;
            if(folderItem.getFolderId() != null){
                existing = folderItemMapper.selectByFolderId(folderItem.getFolderId());
            }else{
                existing = folderItemMapper.selectCourseInfo(folderItem.getCourseId());
            }

            int nextIndex = existing.stream()
                    .map(FolderItem::getOrderIndex)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;
            folderItem.setOrderIndex(nextIndex);
        }
        folderItemMapper.insert(folderItem);
        return folderItem.getId();
    }

    /**
     * 删除
     * Delete by ID
     */
    public void deleteById(Integer id) {
        folderItemMapper.deleteById(id);
    }

    /**
     * 批量删除
     * Delete multiple folder items by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            folderItemMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update folder item
     */
    public void updateById(FolderItem folderItem) {
        folderItemMapper.updateById(folderItem);
    }

    /**
     * 根据 ID 查询
     * Select by ID
     */
    public FolderItem selectById(Integer id) {
        return folderItemMapper.selectById(id);
    }

    /**
     * 根据文件夹 ID 删除所有资源项
     * Delete all items in a folder
     */
    public void deleteByFolderId(Integer folderId) {
        folderItemMapper.deleteByFolderId(folderId);
    }


    /**
     * 查询所有
     * Select all folder items
     */
    public List<FolderItem> selectAll(FolderItem folderItem) {
        return folderItemMapper.selectAll(folderItem);
    }

    /**
     * 查询文件夹下所有资源项
     * Select folder items by folder ID
     */
    public List<FolderItem> selectByFolderId(Integer folderId) {
        return folderItemMapper.selectByFolderId(folderId);
    }

    /**
     * 查询课程信息
     * Select folder items by course ID whose is_course_info is 1
     */
    public List<FolderItem> selectCourseInfo(Integer courseId) { return folderItemMapper.selectCourseInfo(courseId); }

    public List<DiskFiles> selectCourseFilesByCourseId(Integer courseId) {
        return folderItemMapper.selectFolderFilesByCourseId(courseId);
    }
}
