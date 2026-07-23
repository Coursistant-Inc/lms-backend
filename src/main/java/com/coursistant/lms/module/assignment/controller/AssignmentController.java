package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.AssignmentGroupDTO;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.dto.AssignmentDTO;
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
 * Assignment frontend operation API
 **/
@RestController
@RequestMapping("/assignment")
public class AssignmentController {

    @Resource
    private AssignmentService assignmentService;

    private static final Logger logger = Logger.getLogger(AssignmentController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        // logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        // logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增书签
     * Add a new assignment
     */
    @RequiresPermission("assignment:manage")
    @PostMapping("/add")
    public Result add(@RequestBody Assignment assignment,
                      @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("add", assignment.toString());
        ZoneId zone=TimeZoneUtils.resolveZoneId(timezone);
        int assignment_id=assignmentService.add(assignment,zone);
        logResponse("add", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("assignmentId", assignment_id);
        return Result.success(data);
    }

    /**
     * 根据 ID 删除书签
     * Delete a assignment by ID
     */
    @RequiresPermission("assignment:manage")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());

        assignmentService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除书签
     * Batch delete assignments
     */
    @RequiresPermission("assignment:manage")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        assignmentService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新书签
     * Update a assignment
     */
    @RequiresPermission("assignment:manage")
    @PutMapping("/update")
    public Result updateById(@RequestBody Assignment assignment,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("updateById", assignment.toString());
        ZoneId zone=TimeZoneUtils.resolveZoneId(timezone);
        assignmentService.updateById(assignment,zone);
        logResponse("updateById", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:manage")
    @PutMapping("/publishGrade")
    public Result publishGrade(@RequestBody Assignment assignment,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("updateById", assignment.toString());
        ZoneId zone=TimeZoneUtils.resolveZoneId(timezone);
        assignmentService.publishGrade(assignment);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a assignment by ID
     */
    @RequiresPermission("assignment:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        // logRequest("selectById", id.toString());
        ZoneId zone=TimeZoneUtils.resolveZoneId(timezone);
        AssignmentDTO assignment = assignmentService.selectById(id,zone);
        // logResponse("selectById", assignment.toString());
        return Result.success(assignment);
    }

    /**
     * 根据课程 ID 查询该课程下的所有作业（
     * Query all assignments by course ID with timezone conversion
     */
    @RequiresPermission("assignment:view")
    @GetMapping("/selectByCourseId/{id}")
    public Result selectByCourseId(@PathVariable Integer id,
                                   @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        // logRequest("selectByCourseId", id.toString());

        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);

        List<AssignmentGroupDTO> assignments = assignmentService.selectByCourseId(id, zone);

        // logResponse("selectByCourseId", "Total: " + assignments.size());
        return Result.success(assignments);
    }


    /**
     * 查询所有书签
     * Query all assignments
     */
    @RequiresPermission("assignment:view")
    @GetMapping("/selectAll")
    public Result selectAll(Assignment assignment,
                            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        // logRequest("selectAll", assignment != null ? assignment.toString() : "null");
        ZoneId zone=TimeZoneUtils.resolveZoneId(timezone);
        List<Assignment> list = assignmentService.selectAll(assignment,zone);
        // logResponse("selectAll", null);
        return Result.success(list);
    }

}
