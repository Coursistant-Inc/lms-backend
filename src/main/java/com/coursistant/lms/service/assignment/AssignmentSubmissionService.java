package com.coursistant.lms.service.assignment;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.AssignmentSubmission;
import com.coursistant.lms.entity.DTO.AssignmentSubmissionDTO;
import com.coursistant.lms.entity.SubmissionFile;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.assignment.AssignmentSubmissionMapper;
import com.coursistant.lms.service.file.SubmissionFileService;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.List;


@Service
public class AssignmentSubmissionService {

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    
    @Resource
    private SubmissionFileService submissionFileService;


    public void add(AssignmentSubmission assignmentSubmission, List<MultipartFile> files) {
        assignmentSubmissionMapper.insert(assignmentSubmission);
        int submissionId=assignmentSubmission.getId();


        if (ObjectUtil.isNotNull(files)) {
            for (int i=0;i< files.size();i++){
                submissionFileService.add(files.get(i),submissionId);
            }
        }
    }

    /**
     * 删除
     * Delete a assignmentSubmission by ID
     */
    public void deleteById(Integer id) {
        assignmentSubmissionMapper.deleteById(id);
        List<SubmissionFile> submissionFiles=submissionFileService.selectBySubmissionId(id);
        if (ObjectUtil.isNotNull(submissionFiles)) {
            for (int i=0;i< submissionFiles.size();i++){
                submissionFileService.deleteById(submissionFiles.get(i).getId());
            }
        }
    }

    /**
     * 批量删除
     * Delete multiple assignmentSubmissions by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改
     * Update a assignmentSubmission by ID
     */
    public void updateById(AssignmentSubmission assignmentSubmission) {
        assignmentSubmissionMapper.updateById(assignmentSubmission);

    }

    /**
     * 根据ID查询
     * Query a assignmentSubmission by ID
     */
    public AssignmentSubmissionDTO selectById(Integer id, ZoneId timezone) {



        AssignmentSubmission assignmentSubmission =assignmentSubmissionMapper.selectById(id);
        assignmentSubmission.setDate(TimeZoneUtils.fromUtcLocalDateTime(assignmentSubmission.getDate(),timezone));
        AssignmentSubmissionDTO DTO=new AssignmentSubmissionDTO();
        if (ObjectUtil.isNull(DTO)) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }
        BeanUtil.copyProperties(assignmentSubmission, DTO);
        List<SubmissionFile> submissionFiles=submissionFileService.selectBySubmissionId(id);
        DTO.setFiles(submissionFiles);

        return DTO;
    }

    /**
     * 查询所有
     * Query all assignmentSubmissions
     */
    public List<AssignmentSubmission> selectAll(AssignmentSubmission assignmentSubmission1, ZoneId timezone) {
        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        List<AssignmentSubmission> assignmentSubmissions = assignmentSubmissionMapper.selectAll(assignmentSubmission1);
        for (AssignmentSubmission assignmentSubmission:assignmentSubmissions){
            assignmentSubmission.setDate(TimeZoneUtils.fromUtcLocalDateTime(assignmentSubmission.getDate(),timezone));
        }

        return assignmentSubmissions;
    }

}
