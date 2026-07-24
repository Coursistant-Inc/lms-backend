package com.coursistant.lms.module.assignment.service;
import com.coursistant.lms.module.chat.entity.Query;
import com.coursistant.lms.module.quiz.entity.Quiz;



import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.module.assignment.dto.AssignmentGroupDTO;
import com.coursistant.lms.module.assignment.repository.AssignmentItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.assignment.service.AssignmentFileService;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.calendar.entity.CalendarDisplayEvent;
import com.coursistant.lms.module.assignment.dto.AssignmentDTO;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.shared.util.TimeZoneUtils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;

import java.awt.desktop.SystemEventListener;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentFile;


@Service
public class AssignmentService {

    @Resource
    private AssignmentMapper assignmentMapper;


    @Resource
    private QuizMapper quizMapper;

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

        // Enrollment model removed; grade-publish emails are skipped until a new recipient source exists.
        if (row == 1 && Boolean.TRUE.equals(assignment.getGradePublish())) {
            assignmentMapper.selectById(assignment.getId());
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
        // 1. 查询作业
        List<Assignment> assignments = assignmentMapper.selectByCourseId(courseId);

        // 2. 查询 quiz
        List<Quiz> quizzes = quizMapper.selectByCourseId(courseId);

        // 3. Assignment 调整时区
        for (Assignment assignment : assignments) {
            assignment.setStart(TimeZoneUtils.fromUtcLocalDateTime(assignment.getStart(), timezone));
            assignment.setDue(TimeZoneUtils.fromUtcLocalDateTime(assignment.getDue(), timezone));
        }

        // 4. Quiz 调整时区（如果有 start / due）
        for (Quiz quiz : quizzes) {
            //quiz.setStartAt(TimeZoneUtils.fromUtcLocalDateTime(quiz.getStartAt(), timezone));
            quiz.setDueAt(TimeZoneUtils.fromUtcLocalDateTime(quiz.getDueAt(), timezone));
        }

        // 5. 按 courseContentId 分组（作业）
        Map<Integer, List<Assignment>> assignmentGrouped =
                assignments.stream()
                        .collect(Collectors.groupingBy(
                                Assignment::getCourseContentId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        // 6. 按 courseContentId 分组（Quiz）
        Map<Integer, List<Quiz>> quizGrouped =
                quizzes.stream()
                        .collect(Collectors.groupingBy(
                                Quiz::getCourseContentId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        // 7. 合并分组并输出
        List<AssignmentGroupDTO> result = new ArrayList<>();

        for (Map.Entry<Integer, List<Assignment>> entry : assignmentGrouped.entrySet()) {
            Integer courseContentId = entry.getKey();
            List<Assignment> assignmentList = entry.getValue();

            // 找到同一 contentId 下的 quiz（可能没有）
            List<Quiz> quizList = quizGrouped.getOrDefault(courseContentId, new ArrayList<>());

            // 传给 DTO
            result.add(new AssignmentGroupDTO(courseContentId, assignmentList, quizList));
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
