package com.coursistant.lms.service.course;


import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.entity.DTO.FolderDTO;
import com.coursistant.lms.entity.Folder;
import com.coursistant.lms.entity.FolderItem;
import com.coursistant.lms.mapper.file.FolderItemMapper;
import com.coursistant.lms.mapper.file.FolderMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class CourseContentService {

    @Resource
    private FolderMapper folderMapper;

    @Resource
    private FolderItemMapper folderItemMapper;

    /**
     * 新增
     * Add new folder with auto-incremented order index
     */
    public Integer add(Folder folder) {
        if (ObjectUtil.isNull(folder.getOrderIndex())){
            List<Folder> existing = folderMapper.selectByCourseId(folder.getCourseId());
            int nextIndex = existing.stream()
                    .map(Folder::getOrderIndex)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;
            folder.setOrderIndex(nextIndex);
        }
        folderMapper.insert(folder);
        return folder.getId();
    }

    /**
     * 删除
     * Delete by ID
     */
    public void deleteById(Integer id) {
        folderMapper.deleteById(id);
        folderItemMapper.deleteByFolderId(id);
    }

    /**
     * 批量删除
     * Delete multiple folders by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            folderMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update folder
     */
    public void updateById(Folder folder) {
        folderMapper.updateById(folder);
    }

    /**
     * 根据 ID 查询
     * Select by ID
     */
    public Folder selectById(Integer id) {
        return folderMapper.selectById(id);
    }

    /**
     * 查询所有
     * Select all folders
     */
    public List<Folder> selectAll(Folder folder) {
        return folderMapper.selectAll(folder);
    }

    /**
     * 查询课程下所有文件夹
     * Select by course ID
     */
    public List<Folder> selectByCourseId(Integer courseId) {
        return folderMapper.selectByCourseId(courseId);
    }

    /**
     * 查询某课程下所有 Folder 及其 FolderItem
     */
    public List<FolderDTO> getFoldersWithItemsByCourseId(Integer courseId) {
        List<Folder> folders = folderMapper.selectByCourseId(courseId);
        if (folders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> folderIds = folders.stream().map(Folder::getId).collect(Collectors.toList());
        List<FolderItem> allItems = folderIds.stream()
                .flatMap(folderId -> folderItemMapper.selectByFolderId(folderId).stream())
                .collect(Collectors.toList());

        Map<Integer, List<FolderItem>> folderItemMap = allItems.stream()
                .collect(Collectors.groupingBy(FolderItem::getFolderId));

        List<FolderDTO> result = new ArrayList<>();
        for (Folder folder : folders) {
            FolderDTO dto = new FolderDTO();
            dto.setId(folder.getId());
            dto.setCourseId(folder.getCourseId());
            dto.setName(folder.getName());
            dto.setDescription(folder.getDescription());
            dto.setOrderIndex(folder.getOrderIndex());
            dto.setItems(folderItemMap.getOrDefault(folder.getId(), new ArrayList<>()));
            result.add(dto);
        }

        return result;
    }
}