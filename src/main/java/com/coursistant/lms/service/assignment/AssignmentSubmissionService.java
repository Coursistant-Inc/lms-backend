package com.coursistant.lms.service.assignment;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.*;

import com.coursistant.lms.entity.DTO.AssignmentSubmissionDTO;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.assignment.AssignmentMapper;
import com.coursistant.lms.mapper.assignment.AssignmentSubmissionMapper;
import com.coursistant.lms.mapper.assignment.GroupMemberMapper;
import com.coursistant.lms.mapper.course.CourseMapper;
import com.coursistant.lms.mapper.file.AssignmentItemMapper;
import com.coursistant.lms.mapper.file.DiskFilesMapper;
import com.coursistant.lms.mapper.file.SubmissionFileMapper;
import com.coursistant.lms.mapper.user.UserMapper;
import com.coursistant.lms.service.file.SubmissionFileService;
import com.coursistant.lms.utils.EmailUtil;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class AssignmentSubmissionService {

    @Resource
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private AssignmentMapper assignmentMapper;

    @Resource
    private DiskFilesMapper diskFilesMapper;
    @Resource
    private GroupMemberMapper groupMemberMapper;

    @Resource
    private SubmissionFileMapper submissionFileMapper;

    @Resource
    private SubmissionFileService submissionFileService;

    @Resource
    private AssignmentService assignmentService;

    @Resource
    private EmailUtil emailUtil;




    public Integer add(AssignmentSubmission assignmentSubmission) {
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

        if (nowUtc.isAfter(dueTime)) {
            assignmentSubmission.setLate(true);
            Duration diff = Duration.between(nowUtc, dueTime).abs();
            assignmentSubmission.setLateTime(diff.toString());
            //throw new CustomException(ResultCodeEnum.SUBMISSION_DUE_EXPIRED_ERROR);
        }

        if (submissions.size() >= toCheck.getAllowedSubmissionTimes()) {
            throw new CustomException(ResultCodeEnum.SUBMISSION_ATTEMPT_EXCEEDED_ERROR);
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

        Integer result=assignmentSubmission.getId();

        return result;

    }


    //小组作业

    public Integer addAsGroup(AssignmentSubmission assignmentSubmission) {
        int assignmentId = assignmentSubmission.getAssignmentId();
        int studentId = assignmentSubmission.getStudentId();
        Assignment assignmentInfo= assignmentMapper.selectById(assignmentSubmission.getAssignmentId());
        Integer courseId=assignmentInfo.getCourseId();
        ZoneId zone = ZoneId.of("UTC");

        // 查询该学生在该作业下已有提交

        Integer groupId=groupMemberMapper.selectGroupIdByCourseIdAndUserId(courseId,studentId);

        if (groupId == null) {
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }

        List<GroupMember> members=groupMemberMapper.selectByGroupId(groupId);


        List<AssignmentSubmission> submissions =new ArrayList<>();
        for (GroupMember member:members){
            AssignmentSubmission qryItem = new AssignmentSubmission();
            qryItem.setAssignmentId(assignmentId);
            qryItem.setStudentId(member.getUserId());
            List<AssignmentSubmission> indiSubmissions = selectAll(qryItem, zone);
            if (indiSubmissions!=null){
                submissions.addAll(indiSubmissions);
            }

        }



        // 查询该作业允许的提交次数
        Assignment toCheck = assignmentService.selectById(assignmentId, zone);
        // 当前 UTC 时间是否晚于作业截止时间
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime dueTime = toCheck.getDue().atZone(ZoneOffset.UTC);

        if (nowUtc.isAfter(dueTime)) {
            assignmentSubmission.setLate(true);
            Duration diff = Duration.between(nowUtc, dueTime).abs();
            assignmentSubmission.setLateTime(diff.toString());
            //throw new CustomException(ResultCodeEnum.SUBMISSION_DUE_EXPIRED_ERROR);
        }

        /*
        if (submissions.size() >= toCheck.getAllowedSubmissionTimes()) {
            throw new CustomException(ResultCodeEnum.SUBMISSION_ATTEMPT_EXCEEDED_ERROR);
        }*/

        // 将之前该学生的所有提交设为 is_final = false
        if (members != null && !members.isEmpty()) {
            for (GroupMember member : members) {
                assignmentSubmissionMapper.clearFinalFlag(assignmentId, member.getUserId());
            }
        }


        // 当前新提交设为 is_final = true
        assignmentSubmission.setFinal(true);
        assignmentSubmission.setGroupId(groupId);
        assignmentSubmissionMapper.insert(assignmentSubmission);

        // 如果是第一次提交，更新该作业的提交计数
        if (submissions.isEmpty()) {
            Assignment toUpdate = new Assignment();
            toUpdate.setId(assignmentId);
            assignmentService.incrementSubNumById(toUpdate);
        }

        Integer result=assignmentSubmission.getId();

        return result;

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

    public void updateGroupGradeById(AssignmentSubmission assignmentSubmission) {
        if (ObjectUtil.isNull(assignmentSubmission.getGrade())){
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        assignmentSubmissionMapper.updateById(assignmentSubmission);
        calculateStats(assignmentSubmission.getAssignmentId());
        /*
        Assignment assignmentInfo= assignmentMapper.selectById(assignmentSubmission.getAssignmentId());
        Integer courseId=assignmentInfo.getCourseId();
        int studentId = assignmentSubmission.getStudentId();

        Integer groupId=groupMemberMapper.selectGroupIdByCourseIdAndUserId(courseId,studentId);

        if (groupId == null) {
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }

        List<GroupMember> members=groupMemberMapper.selectByGroupId(groupId);

        if (members != null && !members.isEmpty()) {
            for (GroupMember member : members) {
                assignmentSubmissionMapper.updateById(assignmentSubmission);
            }
        }*/



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

        List<AssignmentSubmission> assignmentSubmissions = assignmentSubmissionMapper.selectAll(assignmentSubmission1);
        for (AssignmentSubmission assignmentSubmission:assignmentSubmissions){
            assignmentSubmission.setDate(TimeZoneUtils.fromUtcLocalDateTime(assignmentSubmission.getDate(),timezone));
        }

        return assignmentSubmissions;
    }


    public AssignmentSubmission selectFinalSubmissionByUserId(Integer assignmentId, Integer studentId, ZoneId timezone) {

        AssignmentSubmission assignmentSubmission = assignmentSubmissionMapper.selectFinalByAssignmentIdAndUserId(assignmentId,studentId);
        assignmentSubmission.setDate(TimeZoneUtils.fromUtcLocalDateTime(assignmentSubmission.getDate(),timezone));


        return assignmentSubmission;
    }

    public AssignmentSubmission selectGroupFinalSubmissionByUserId(Integer assignmentId, Integer studentId, ZoneId timezone) {

        Assignment assignmentInfo= assignmentMapper.selectById(assignmentId);
        Integer courseId=assignmentInfo.getCourseId();
        Integer groupId=groupMemberMapper.selectGroupIdByCourseIdAndUserId(courseId,studentId);
        AssignmentSubmission assignmentSubmission = assignmentSubmissionMapper.selectFinalByAssignmentIdAndGroupId(assignmentId,groupId);
        assignmentSubmission.setDate(TimeZoneUtils.fromUtcLocalDateTime(assignmentSubmission.getDate(),timezone));


        return assignmentSubmission;
    }


    public void sendSubmissionEmail(Integer submissionId) {
        AssignmentSubmission assignmentSubmission=assignmentSubmissionMapper.selectById(submissionId);
        User student=userMapper.selectById(assignmentSubmission.getStudentId());
        Assignment assignment=assignmentMapper.selectById(assignmentSubmission.getAssignmentId());
        Course course=courseMapper.selectById(assignment.getCourseId());
        List<SubmissionFile> submissionFiles=submissionFileMapper.selectFileBySubmissionId(assignmentSubmission.getId());
        List<String> fileNames=new ArrayList<>();
        for  (SubmissionFile submissionFile:submissionFiles){
            String fileName=diskFilesMapper.selectById(submissionFile.getFileId()).getName();
            Double sizeKb = diskFilesMapper.selectById(submissionFile.getFileId()).getSize();

            // 转换成 MB，保留两位小数
            double sizeMb = sizeKb / 1024.0;
            String sizeStr = String.format("%.2f MB", sizeMb);

            // 拼接结果
            String fileWithSize = fileName + " (" + sizeStr + ")";
            fileNames.add(fileWithSize);
        }

        String studentName=student.getUsername();
        String email=student.getEmail();
        LocalDateTime utcTime=assignmentSubmission.getDate();
        ZoneId laZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime laTime = utcTime.atZone(ZoneOffset.UTC).withZoneSameInstant(laZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a z", Locale.ENGLISH);
        String formatted = laTime.format(formatter);

        // 拼接文件名列表
        StringBuilder fileListBuilder = new StringBuilder();
        for (String fileName : fileNames) {
            if (fileListBuilder.length() > 0) {
                fileListBuilder.append(", ");
            }
            fileListBuilder.append(fileName);
        }
        String filesList = fileListBuilder.toString();



        // 组装邮件正文
        String subject = "Submission Confirmation — " + assignment.getTitle()
                + " (ID " + assignmentSubmission.getId() + ")";
        String emailBody = ""
                + "Hi " + studentName + ",\n\n"
                + "This email confirms that your submission to " + assignment.getTitle() + " was successful.\n\n"
                + "Submission ID: " + assignmentSubmission.getId() + "\n"
                + "Received: " + formatted +"\n"
                + "Course/Section: " + course.getName() + "\n"
                + "File(s): " + filesList + "\n\n"
                + "If you did not intend to submit, or you need to make changes, please check "
                + "the assignment’s resubmission policy and deadline on the LMS page.\n\n"
                + "Best regards,\n"
                + "Coursistant xLearn Team";

        emailUtil.sendEmail(email,subject,emailBody);

    }

}
