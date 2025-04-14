package com.coursistant.individual.service.assignment;


import cn.hutool.core.bean.BeanUtil;
import com.coursistant.individual.service.file.AssignmentFileService;
import com.coursistant.individual.common.enums.ResultCodeEnum;
import com.coursistant.individual.entity.Assignment;
import com.coursistant.individual.entity.AssignmentFile;
import com.coursistant.individual.entity.DTO.AssignmentDTO;
import com.coursistant.individual.exception.CustomException;
import com.coursistant.individual.mapper.assignment.AssignmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;


@Service
public class AssignmentService {

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private AssignmentFileService assignmentFileService;


    public void add(Assignment assignment, List<MultipartFile> files) {
        assignmentMapper.insert(assignment);
        int assignmentId=assignment.getId();
        for (int i=0;i< files.size();i++){
            assignmentFileService.add(files.get(i),assignmentId);
        }

    }

    /**
     * 删除
     * Delete a assignment by ID
     */
    public void deleteById(Integer id) {
        assignmentMapper.deleteById(id);

        List<AssignmentFile> assignmentFiles=assignmentFileService.selectByAssignmentId(id);
        for (int i=0;i< assignmentFiles.size();i++){
            assignmentFileService.deleteById(assignmentFiles.get(i).getId());
        }


    }

    /**
     * 批量删除
     * Delete multiple assignments by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改
     * Update a assignment by ID
     */
    public void updateById(Assignment assignment) {
        assignmentMapper.updateById(assignment);

    }

    /**
     * 根据ID查询
     * Query a assignment by ID
     */
    public AssignmentDTO selectById(Integer id) {


        Assignment assignment = assignmentMapper.selectById(id);
        AssignmentDTO assignmentDTO=new AssignmentDTO();
        if (assignment == null) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }
        BeanUtil.copyProperties(assignment, assignmentDTO);
        List<AssignmentFile> assignmentFiles=assignmentFileService.selectByAssignmentId(id);
        assignmentDTO.setFiles(assignmentFiles);

        return assignmentDTO;
    }

    /**
     * 查询所有
     * Query all assignments
     */
    public List<Assignment> selectAll(Assignment assignment) {
        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        List<Assignment> assignmentes = assignmentMapper.selectAll(assignment);

        return assignmentes;
    }

}
