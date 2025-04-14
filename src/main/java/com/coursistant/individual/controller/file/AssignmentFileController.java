package com.coursistant.individual.controller.file;

import com.coursistant.individual.service.file.AssignmentFileService;
import com.coursistant.individual.common.Result;
import com.coursistant.individual.entity.AssignmentFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

/**
 * 部门信息表前端操作接口
 * AssignmentFile frontend operation API
 **/
@RestController
@RequestMapping("/assignmentFile")
public class AssignmentFileController {

    @Resource
    private AssignmentFileService assignmentFileService;

    private static final Logger logger = Logger.getLogger(AssignmentFileController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增书签
     * Add a new assignmentFile
     */
    @PostMapping("/add")
    public Result add(MultipartFile file,Integer assignmentId) {
        logRequest("add", assignmentId.toString());
        assignmentFileService.add(file, assignmentId);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除书签
     * Delete a assignmentFile by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        assignmentFileService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除书签
     * Batch delete assignmentFiles
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        assignmentFileService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新书签
     * Update a assignmentFile
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody AssignmentFile assignmentFile) {
        logRequest("updateById", assignmentFile.toString());
        assignmentFileService.updateById(assignmentFile);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a assignmentFile by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        AssignmentFile assignmentFile = assignmentFileService.selectById(id);
        logResponse("selectById", assignmentFile.toString());
        return Result.success(assignmentFile);
    }

    /**
     * 查询所有书签
     * Query all assignmentFiles
     */
    @GetMapping("/selectAll")
    public Result selectAll(AssignmentFile assignmentFile) {
        logRequest("selectAll", assignmentFile != null ? assignmentFile.toString() : "null");
        List<AssignmentFile> list = assignmentFileService.selectAll(assignmentFile);
        logResponse("selectAll", null);
        return Result.success(list);
    }

}
