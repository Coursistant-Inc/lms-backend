package com.coursistant.lms.service.assignment;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.entity.CalendarDisplayEvent;
import com.coursistant.lms.entity.Learn;
import com.coursistant.lms.service.course.LearnService;
import com.coursistant.lms.service.file.AssignmentFileService;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.entity.AssignmentFile;
import com.coursistant.lms.entity.DTO.AssignmentDTO;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.assignment.AssignmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class AssignmentService {

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private AssignmentFileService assignmentFileService;

    @Resource
    private LearnService learnService;


    public void add(Assignment assignment, List<MultipartFile> files) {
        if (ObjectUtil.isNull(assignment.getSubmissionNum())) {
            assignment.setSubmissionNum(0);
        }
        if (ObjectUtil.isNull(assignment.getAllowedSubmissionTimes())) {
            assignment.setAllowedSubmissionTimes(1);
        }
        if (ObjectUtil.isNull(assignment.getHighestGrade())) {
            assignment.setHighestGrade(0);
        }
        if (ObjectUtil.isNull(assignment.getLowestGrade())) {
            assignment.setLowestGrade(0);
        }
        if (ObjectUtil.isNull(assignment.getAverageGrade())) {
            assignment.setAverageGrade(0);
        }
        if (ObjectUtil.isNull(assignment.getGradePublish())) {
            assignment.setGradePublish(false);
        }
        assignmentMapper.insert(assignment);
        int assignmentId=assignment.getId();
        //System.out.println("assignment id: " + Integer.toString(assignmentId));
        if (ObjectUtil.isNotNull(files)) {
            for (int i=0;i< files.size();i++){
                assignmentFileService.add(files.get(i),assignmentId);
            }
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
     * 增加 Assignment Submission Number
     * Increment the submission number by ID
     */
    public void incrementSubNumById(Assignment assignment) {
        assignmentMapper.incrementSubNumById(assignment);

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

    public List<Assignment> selectByCourseId(Integer id){
        List<Assignment> assignment=assignmentMapper.selectByCourseId(id);

        return  assignment;
    }

    /**
     * 查询所有
     * Query all assignments
     */
    public List<Assignment> selectAll(Assignment assignment) {
        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        List<Assignment> assignments = assignmentMapper.selectAll(assignment);

        return assignments;
    }

    public List<CalendarDisplayEvent> selectAssignmentByUserAndTimeRange(Integer userId, LocalDateTime start, LocalDateTime end) {
        List<CalendarDisplayEvent> result = new ArrayList<>();

        List<Learn> dblearn = learnService.selectByStudentId(userId);
        if (dblearn == null || dblearn.isEmpty()) {
            return result;
        }

        for (Learn singleLearn : dblearn) {
            List<Assignment> assignments = assignmentMapper.selectByCourseId(singleLearn.getCourseId());
            for (Assignment assignment : assignments) {

                // ✅ 过滤：截止时间为空，或不在指定时间范围内则跳过
                if (assignment.getDue() == null) {
                    continue;
                }
                if (assignment.getDue().isBefore(start) || assignment.getDue().isAfter(end)) {
                    continue;
                }

                CalendarDisplayEvent single = new CalendarDisplayEvent();
                single.setTimezone(assignment.getTimezone());
                single.setAllDay(false);
                single.setStart(assignment.getDue().minusMinutes(5));
                single.setEnd(assignment.getDue());
                single.setType("assignment");
                single.setTitle(assignment.getTitle());
                single.setSourceId(assignment.getId());

                result.add(single);
            }
        }

        return result;
    }


}
