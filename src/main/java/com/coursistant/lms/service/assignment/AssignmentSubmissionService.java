package com.coursistant.lms.service.assignment;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.entity.AssignmentSubmission;
import com.coursistant.lms.entity.DTO.AssignmentDTO;
import com.coursistant.lms.entity.DTO.AssignmentSubmissionDTO;
import com.coursistant.lms.entity.SubmissionFile;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.assignment.AssignmentSubmissionMapper;
import com.coursistant.lms.service.file.SubmissionFileService;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class AssignmentSubmissionService {

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    
    @Resource
    private SubmissionFileService submissionFileService;

    @Resource
    private AssignmentService assignmentService;


    public void add(AssignmentSubmission assignmentSubmission, List<MultipartFile> files) {
        int assignmentId = assignmentSubmission.getAssignmentId();
        int studentId = assignmentSubmission.getStudentId();
        ZoneId zone = ZoneId.of("UTC");

        // 查询该学生在该作业下已有提交
        AssignmentSubmission qryItem = new AssignmentSubmission();
        qryItem.setAssignmentId(assignmentId);
        qryItem.setStudentId(studentId);
        List<AssignmentSubmission> submissions = selectAll(qryItem, zone);

        // 查询该作业允许的提交次数
        Assignment toCheck = assignmentService.selectById(assignmentId, zone);
        // 当前 UTC 时间是否晚于作业截止时间
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime dueTime = toCheck.getDue().atZone(ZoneOffset.UTC);

        if (nowUtc.isAfter(dueTime) || submissions.size() >= toCheck.getAllowedSubmissionTimes()) {
            throw new CustomException(ResultCodeEnum.SUBMISSION_NOT_VALID_ERROR);
        }

        // 将之前该学生的所有提交设为 is_final = false
        assignmentSubmissionMapper.clearFinalFlag(assignmentId, studentId);

        // 当前新提交设为 is_final = true
        assignmentSubmission.setFinal(true);
        assignmentSubmissionMapper.insert(assignmentSubmission);

        // 如果是第一次提交，更新该作业的提交计数
        if (submissions.isEmpty()) {
            Assignment toUpdate = new Assignment();
            toUpdate.setId(assignmentId);
            assignmentService.incrementSubNumById(toUpdate);
        }


        // 保存上传文件
        int submissionId = assignmentSubmission.getId();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                submissionFileService.add(file, submissionId);
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
     * 修改成绩
     * Update a assignmentSubmission by ID
     */
    public void updateGradeById(AssignmentSubmission assignmentSubmission) {
        if (ObjectUtil.isNull(assignmentSubmission.getGrade())){
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        assignmentSubmissionMapper.updateById(assignmentSubmission);
        calculateStats(assignmentSubmission.getAssignmentId());

    }

    /**
     * 更新作业成绩统计信息（最高分、最低分、平均分）
     * Update score statistics (max, min, average) for a specific assignment
     */

    public void calculateStats(Integer assignmentId) {
        List<AssignmentSubmission> submissions=assignmentSubmissionMapper.selectFinalGradedByAssignmentId(assignmentId);
        if (submissions == null || submissions.isEmpty()) {
            return;
        }
        // 提取所有非 null 的分数列表
        List<BigDecimal> grades = submissions.stream()
                .map(AssignmentSubmission::getGrade)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (grades.isEmpty()) {
            return;
        }

        BigDecimal max = grades.stream().max(Comparator.naturalOrder()).get();
        BigDecimal min = grades.stream().min(Comparator.naturalOrder()).get();
        BigDecimal sum = grades.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP);


        Assignment updateInfo=new Assignment();
        updateInfo.setLowestGrade(min);
        updateInfo.setHighestGrade(max);
        updateInfo.setAverageGrade(avg);
        updateInfo.setId(assignmentId);

        assignmentService.updateById(updateInfo,null);



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
