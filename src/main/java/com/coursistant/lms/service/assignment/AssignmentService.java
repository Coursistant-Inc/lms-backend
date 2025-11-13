package com.coursistant.lms.service.assignment;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.entity.*;

import com.coursistant.lms.entity.DTO.AssignmentGroupDTO;
import com.coursistant.lms.mapper.course.CourseMapper;
import com.coursistant.lms.mapper.course.LearnMapper;
import com.coursistant.lms.mapper.file.AssignmentItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.CalendarDisplayEvent;
import com.coursistant.lms.entity.DTO.AssignmentDTO;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.assignment.AssignmentMapper;
import com.coursistant.lms.service.course.LearnService;
import com.coursistant.lms.service.file.AssignmentFileService;
import com.coursistant.lms.utils.EmailUtil;
import com.coursistant.lms.utils.TimeZoneUtils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;

import java.awt.desktop.SystemEventListener;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class AssignmentService {

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private EmailUtil emailUtil;

    @Resource
    private LearnMapper     learnMapper;

    @Resource
    private AssignmentFileService assignmentFileService;

    @Resource
    private AssignmentItemMapper assignmentItemMapper;



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

        assignmentItemMapper.deleteByAssignmentId(id);


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

    public void publishGrade(Assignment assignment) {
        int row= assignmentMapper.updateById(assignment);

        if (row==1 && assignment.getGradePublish()==true){
            Assignment updated=assignmentMapper.selectById(assignment.getId());
            List<String> emails=learnMapper.selectEmailsByCourseId(updated.getCourseId());
            Course course=courseMapper.selectById(updated.getCourseId());
            if (emails != null && !emails.isEmpty()) {
                // 去重 & 过滤空邮箱
                List<String> targets = emails.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList();

                String subject = "Grade Released — " + updated.getTitle();
                String content = ""
                        + "Your grade for **" + updated.getTitle() + "** in the course **" + course.getName() + "** has been released.\n\n"
                        + "Best regards,\n"
                        + "**Coursistant xLearn Team**";

                for (String to : targets) {
                    try {
                        emailUtil.sendEmail(to, subject, content);
                    } catch (Exception e) {
                        // 打印错误日志，但不中断后续邮件发送
                        System.out.print("Failed to send email to: " + to);
                    }
                }
            }
        }
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
        assignmentDTO.setGroup(assignment.getGroup());
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
