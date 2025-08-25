package com.coursistant.lms.service.assignment;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.entity.AssignmentGroup;
import com.coursistant.lms.entity.CalendarDisplayEvent;

import com.coursistant.lms.entity.DTO.AssignmentGroupDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.entity.AssignmentFile;
import com.coursistant.lms.entity.CalendarDisplayEvent;
import com.coursistant.lms.entity.DTO.AssignmentDTO;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.assignment.AssignmentMapper;
import com.coursistant.lms.service.course.LearnService;
import com.coursistant.lms.service.file.AssignmentFileService;
import com.coursistant.lms.utils.TimeZoneUtils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;

import java.awt.desktop.SystemEventListener;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class AssignmentService {

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private AssignmentFileService assignmentFileService;



    public int add(Assignment assignment, ZoneId timezone) {
        if (ObjectUtil.isNull(assignment.getSubmissionNum())) {
            assignment.setSubmissionNum(0);
        }
        if (ObjectUtil.isNull(assignment.getAllowedSubmissionTimes())) {
            assignment.setAllowedSubmissionTimes(1);
        }
        if (ObjectUtil.isNull(assignment.getGradePublish())) {
            assignment.setGradePublish(false);
        }
        assignment.setStart(TimeZoneUtils.toUtcLocalDateTime(assignment.getStart(),timezone));
        assignment.setDue(TimeZoneUtils.toUtcLocalDateTime(assignment.getDue(),timezone));

        assignmentMapper.insert(assignment);
        int assignmentId=assignment.getId();

        return assignmentId;

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
    public void updateById(Assignment assignment, ZoneId timezone) {

        if (assignment.getStart() != null) {
            assignment.setStart(TimeZoneUtils.toUtcLocalDateTime(assignment.getStart(), timezone));
        }

        if (assignment.getDue() != null) {
            assignment.setDue(TimeZoneUtils.toUtcLocalDateTime(assignment.getDue(), timezone));
        }

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
    public AssignmentDTO selectById(Integer id, ZoneId timezone) {


        Assignment assignment = assignmentMapper.selectById(id);
        AssignmentDTO assignmentDTO=new AssignmentDTO();
        if (assignment == null) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_NOT_EXIST_ERROR);
        }
        assignment.setStart(TimeZoneUtils.fromUtcLocalDateTime(assignment.getStart(),timezone));
        assignment.setDue(TimeZoneUtils.fromUtcLocalDateTime(assignment.getDue(),timezone));

        BeanUtil.copyProperties(assignment, assignmentDTO);
        List<AssignmentFile> assignmentFiles=assignmentFileService.selectByAssignmentId(id);
        assignmentDTO.setFiles(assignmentFiles);

        return assignmentDTO;
    }

    public List<AssignmentGroupDTO> selectByCourseId(Integer courseId, ZoneId timezone) {
        List<Assignment> assignments = assignmentMapper.selectByCourseId(courseId);

        for (Assignment assignment : assignments) {
            assignment.setStart(TimeZoneUtils.fromUtcLocalDateTime(assignment.getStart(), timezone));
            assignment.setDue(TimeZoneUtils.fromUtcLocalDateTime(assignment.getDue(), timezone));
        }

        Map<Integer, List<Assignment>> grouped = assignments.stream()
                .collect(Collectors.groupingBy(Assignment::getCourseContentId, LinkedHashMap::new, Collectors.toList()));

        List<AssignmentGroupDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Assignment>> entry : grouped.entrySet()) {
            result.add(new AssignmentGroupDTO(entry.getKey(), entry.getValue()));
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }

        return result;
    }




    /**
     * 查询所有
     * Query all assignments
     */
    public List<Assignment> selectAll(Assignment assignment1, ZoneId timezone) {
        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        List<Assignment> assignments = assignmentMapper.selectAll(assignment1);

        for (Assignment assignment:assignments){
            assignment.setStart(TimeZoneUtils.fromUtcLocalDateTime(assignment.getStart(),timezone));
            assignment.setDue(TimeZoneUtils.fromUtcLocalDateTime(assignment.getDue(),timezone));
        }


        return assignments;
    }

    public List<AssignmentDTO> selectAssignmentsByCourseAndUserId(Integer userId, Integer courseId)
    {
        List<AssignmentDTO> assignmentDetails = assignmentMapper.selectAssignmentsByCourseAndUserId(userId,courseId);
        
        return assignmentDetails;
    }

    public List<CalendarDisplayEvent> selectAssignmentByUserAndTimeRange(Integer userId, LocalDateTime start, LocalDateTime end, ZoneId timezone) {
        start=TimeZoneUtils.toUtcLocalDateTime(start,timezone);
        end=TimeZoneUtils.toUtcLocalDateTime(end,timezone);
        List<Assignment> assignments = assignmentMapper.selectAssignmentsByUserAndTimeRange(userId, start, end);
        List<CalendarDisplayEvent> result = new ArrayList<>();

        for (Assignment assignment : assignments) {
            LocalDateTime localtime = TimeZoneUtils.fromUtcLocalDateTime(assignment.getDue(), timezone);

            CalendarDisplayEvent event = new CalendarDisplayEvent();
            event.setTimezone(timezone.toString());
            event.setAllDay(false);
            event.setStart(localtime.minusMinutes(5));
            event.setEnd(localtime);
            event.setType("assignment");
            event.setTitle(assignment.getTitle());
            event.setSourceId(assignment.getId());

            result.add(event);
        }

        return result;
    }


}
