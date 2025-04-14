package com.coursistant.individual.controller.assignment;

import com.coursistant.individual.service.assignment.AssignmentService;
import com.coursistant.individual.common.Result;
import com.coursistant.individual.entity.Assignment;
import com.coursistant.individual.entity.DTO.AssignmentDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

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
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增书签
     * Add a new assignment
     */
    @PostMapping("/add")
    public Result add(@ModelAttribute Assignment assignment,
                      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        logRequest("add", assignment.toString());
        assignmentService.add(assignment,files);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除书签
     * Delete a assignment by ID
     */
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
    @PutMapping("/update")
    public Result updateById(@RequestBody Assignment assignment) {
        logRequest("updateById", assignment.toString());
        assignmentService.updateById(assignment);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a assignment by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        AssignmentDTO assignment = assignmentService.selectById(id);
        logResponse("selectById", assignment.toString());
        return Result.success(assignment);
    }

    /**
     * 查询所有书签
     * Query all assignments
     */
    @GetMapping("/selectAll")
    public Result selectAll(Assignment assignment) {
        logRequest("selectAll", assignment != null ? assignment.toString() : "null");
        List<Assignment> list = assignmentService.selectAll(assignment);
        logResponse("selectAll", null);
        return Result.success(list);
    }

}
