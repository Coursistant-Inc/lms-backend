package com.coursistant.lms.controller.assignment;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.entity.AssignmentSubmission;
import com.coursistant.lms.entity.DTO.AssignmentDTO;
import com.coursistant.lms.entity.DTO.AssignmentSubmissionDTO;
import com.coursistant.lms.service.assignment.AssignmentService;
import com.coursistant.lms.service.assignment.AssignmentSubmissionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

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
     * 新增书签
     * Add a new assignmentSubmission
     */
    @PostMapping("/add")
    public Result add(@ModelAttribute AssignmentSubmission assignmentSubmission,
                      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        logRequest("add", assignmentSubmission.toString());
        int assignmentId = assignmentSubmission.getAssignmentId();
        AssignmentSubmission qryItem = new AssignmentSubmission();
        qryItem.setAssignmentId(assignmentSubmission.getAssignmentId());
        qryItem.setStudentId(assignmentSubmission.getStudentId());
        List<AssignmentSubmission> submissions = assignmentSubmissionService.selectAll(qryItem);
        AssignmentDTO tocheck = assignmentService.selectById(assignmentId);
        if (submissions.size() >= tocheck.getAllowedSubmissionTimes()){
            return Result.error(ResultCodeEnum.SUBMISSION_NOT_VALID_ERROR);
        }
        
        if (submissions.size() == 0) {
            Assignment toUpdate = new Assignment();
            toUpdate.setId(assignmentSubmission.getAssignmentId());
            assignmentService.incrementSubNumById(toUpdate);
        }
        assignmentSubmissionService.add(assignmentSubmission,files);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除书签
     * Delete a assignmentSubmission by ID
     */
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
    @PutMapping("/update")
    public Result updateById(@RequestBody AssignmentSubmission assignmentSubmission) {
        logRequest("updateById", assignmentSubmission.toString());
        assignmentSubmissionService.updateById(assignmentSubmission);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a assignmentSubmission by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        AssignmentSubmissionDTO assignmentSubmission = assignmentSubmissionService.selectById(id);
        logResponse("selectById", assignmentSubmission.toString());
        return Result.success(assignmentSubmission);
    }

    /**
     * 查询所有书签
     * Query all assignmentSubmissions
     */
    @GetMapping("/selectAll")
    public Result selectAll(AssignmentSubmission assignmentSubmission) {
        logRequest("selectAll", assignmentSubmission != null ? assignmentSubmission.toString() : "null");
        List<AssignmentSubmission> list = assignmentSubmissionService.selectAll(assignmentSubmission);
        logResponse("selectAll", null);
        return Result.success(list);
    }

}
