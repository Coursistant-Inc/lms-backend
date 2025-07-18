package com.coursistant.lms.service.course;


import com.coursistant.lms.entity.FolderItem;
import com.coursistant.lms.mapper.file.FolderItemMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
}
