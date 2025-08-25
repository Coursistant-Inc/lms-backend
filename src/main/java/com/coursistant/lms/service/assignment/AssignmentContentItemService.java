package com.coursistant.lms.service.assignment;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.AssignmentItem;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.file.AssignmentItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;


@Service
public class AssignmentContentItemService {

    @Resource
    private AssignmentItemMapper assignmentItemMapper;

    private static final String filePath = System.getProperty("user.dir") + "/disk/assignment/";

    /**
     * 新增
     * Add new assignment item
     */
    public Integer add(AssignmentItem assignmentItem) {
        if (ObjectUtil.isNull(assignmentItem.getOrderIndex())){
            List<AssignmentItem> existing = null;
            if(assignmentItem.getAssignmentId() != null){
                existing = assignmentItemMapper.selectByAssignmentId(assignmentItem.getAssignmentId());
            }else{
                existing = assignmentItemMapper.selectCourseInfo(assignmentItem.getAssignmentId());
            }

            int nextIndex = existing.stream()
                    .map(AssignmentItem::getOrderIndex)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(-1) + 1;
            assignmentItem.setOrderIndex(nextIndex);
        }

        assignmentItemMapper.insert(assignmentItem);
        return assignmentItem.getId();
    }

    /**
     * 删除
     * Delete by ID
     */
    public void deleteById(Integer id) {
        assignmentItemMapper.deleteById(id);
    }

    /**
     * 批量删除
     * Delete multiple assignment items by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            assignmentItemMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update assignment item
     */
    public void updateById(AssignmentItem assignmentItem) {
        assignmentItemMapper.updateById(assignmentItem);
    }

    /**
     * 根据 ID 查询
     * Select by ID
     */
    public AssignmentItem selectById(Integer id) {
        return assignmentItemMapper.selectById(id);
    }

    /**
     * 根据文件夹 ID 删除所有资源项
     * Delete all items in a assignment
     */
    public void deleteByAssignmentId(Integer assignmentId) {
        assignmentItemMapper.deleteByAssignmentId(assignmentId);
    }


    /**
     * 查询所有
     * Select all assignment items
     */
    public List<AssignmentItem> selectAll(AssignmentItem assignmentItem) {
        return assignmentItemMapper.selectAll(assignmentItem);
    }

    /**
     * 查询文件夹下所有资源项
     * Select assignment items by assignment ID
     */
    public List<AssignmentItem> selectByAssignmentId(Integer assignmentId) {
        return assignmentItemMapper.selectByAssignmentId(assignmentId);
    }

    /**
     * 查询课程信息
     * Select assignment items by course ID whose is_course_info is 1
     */
    public List<AssignmentItem> selectCourseInfo(Integer courseId) {
        return assignmentItemMapper.selectCourseInfo(courseId); }
}
