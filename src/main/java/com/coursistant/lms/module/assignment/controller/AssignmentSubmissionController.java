package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import com.coursistant.lms.module.assignment.dto.AssignmentSubmissionDTO;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.module.assignment.service.AssignmentSubmissionService;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import com.coursistant.lms.shared.security.RequiresPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 部门信息表前端操作接口
 * AssignmentSubmission frontend operation API
 **/
@RestController
@RequestMapping("/assignmentSubmission")
public class AssignmentSubmissionController {

    @Resource
    private AssignmentSubmissionService assignmentSubmissionService;
    @Resource
    private AssignmentService assignmentService;

    private static final Logger logger = Logger.getLogger(AssignmentSubmissionController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增
     * Add a new assignmentSubmission
     */
    @RequiresPermission("assignment:submit")
    @PostMapping("/add")
    public Result add(@RequestBody AssignmentSubmission assignmentSubmission,

                      @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("add", assignmentSubmission.toString());

        Integer submissionId= assignmentSubmissionService.add(assignmentSubmission);
        logResponse("add", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("submissionId", submissionId);
        return Result.success(data);
    }

    @RequiresPermission("assignment:submit")
    @PostMapping("/addAsGroup")
    public Result addAsGroup(@RequestBody AssignmentSubmission assignmentSubmission,
                      @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("addAsGroup", assignmentSubmission.toString());

        Integer submissionId= assignmentSubmissionService.addAsGroup(assignmentSubmission);
        logResponse("addAsGroup", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("submissionId", submissionId);
        return Result.success(data);
    }

    /**
     * 根据 ID 删除书签
     * Delete a assignmentSubmission by ID
     */
    @RequiresPermission("assignment:submit")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        assignmentSubmissionService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除书签
     * Batch delete assignmentSubmissions
     */
    @RequiresPermission("assignment:submit")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        assignmentSubmissionService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新书签
     * Update a assignmentSubmission
     */
    @RequiresPermission("assignment:manage")
    @PutMapping("/update")
    public Result updateById(@RequestBody AssignmentSubmission assignmentSubmission) {
        logRequest("updateById", assignmentSubmission.toString());
        assignmentSubmissionService.updateById(assignmentSubmission);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 更新
     * Update grade
     */
    @RequiresPermission("assignment:manage")
    @PutMapping("/updateGrade")
    public Result updateGradeById(@RequestBody AssignmentSubmission assignmentSubmission) {
        logRequest("updateById", assignmentSubmission.toString());
        assignmentSubmissionService.updateGradeById(assignmentSubmission);
        logResponse("updateById", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:manage")
    @PutMapping("/updateGroupGrade")
    public Result updateGroupGradeById(@RequestBody AssignmentSubmission assignmentSubmission) {
        logRequest("updateGroupGradeById", assignmentSubmission.toString());
        assignmentSubmissionService.updateGroupGradeById(assignmentSubmission);
        logResponse("updateGroupGradeById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a assignmentSubmission by ID
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectById", id.toString());
        ZoneId zone=TimeZoneUtils.resolveZoneId(timezone);
        AssignmentSubmissionDTO assignmentSubmission = assignmentSubmissionService.selectById(id,zone);
        logResponse("selectById", assignmentSubmission.toString());
        return Result.success(assignmentSubmission);
    }

    /**
     * 查询所有书签
     * Query all assignmentSubmissions
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectAll")
    public Result selectAll(AssignmentSubmission assignmentSubmission,
                            @RequestHeader(value = "X-Timezone", required = false) String timezone){
        logRequest("selectAll", assignmentSubmission != null ? assignmentSubmission.toString() : "null");
        ZoneId zone=TimeZoneUtils.resolveZoneId(timezone);
        List<AssignmentSubmission> list = assignmentSubmissionService.selectAll(assignmentSubmission,zone);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    /**
     * 查询学生在课程下的最终提交记录
     * Query all final assignment submissions for a student in a specific course
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectFinalByUserAndCourse")
    public Result selectFinalByUserAndCourse(@RequestParam Integer courseId,
                                             @RequestParam Integer studentId,
                                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectFinalByUserAndCourse", "courseId=" + courseId + ", studentId=" + studentId);
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<AssignmentSubmission> submissions = assignmentSubmissionService.selectFinalByUserAndCourse(courseId, studentId, zone);
        logResponse("selectFinalByUserAndCourse", "count=" + submissions.size());
        return Result.success(submissions);
    }



    /**
     * 查询指定 assignmentId + studentId 的最终提交
     * Get the final submission by assignmentId and studentId
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectFinalByUser")
    public Result selectFinalByUser(@RequestParam("assignmentId") Integer assignmentId,
                                    @RequestParam("studentId") Integer studentId,
                                    @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectFinalByUser", "assignmentId=" + assignmentId + ", studentId=" + studentId);
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        AssignmentSubmission submission = assignmentSubmissionService.selectFinalSubmissionByUserId(assignmentId, studentId, zone);
        logResponse("selectFinalByUser", submission != null ? submission.toString() : "null");
        return Result.success(submission);
    }

    /**
     * 查询指定 assignmentId + groupId 的最终提交
     * Get the final submission by assignmentId and groupId
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectGroupFinalByUser")
    public Result selectGroupFinalByUser(@RequestParam("assignmentId") Integer assignmentId,
                                         @RequestParam("studentId") Integer studentId,
                                     @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectFinalByGroup", "assignmentId=" + assignmentId + ", groupId=" + studentId);
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        AssignmentSubmission submission = assignmentSubmissionService.selectGroupFinalSubmissionByUserId(assignmentId, studentId, zone);
        logResponse("selectFinalByGroup", submission != null ? submission.toString() : "null");
        return Result.success(submission);
    }



    /**
     * 发送提交成功的通知邮件
     * Send submission confirmation email
     */
    @RequiresPermission("assignment:submit")
    @PostMapping("/sendSubmissionEmail/{submissionId}")
    public Result sendSubmissionEmail(@PathVariable("submissionId") Integer submissionId) {
        logRequest("sendSubmissionEmail", "submissionId=" + submissionId);

        // 调用 service 逻辑（内部完成邮件组装和发送）
        assignmentSubmissionService.sendSubmissionEmail(submissionId);

        logResponse("sendSubmissionEmail", "success");
        return Result.success();
    }


}
