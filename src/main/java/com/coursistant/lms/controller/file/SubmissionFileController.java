package com.coursistant.lms.controller.file;

import com.coursistant.lms.service.file.SubmissionFileService;
import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.SubmissionFile;
import com.coursistant.lms.annotation.RequiresPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

/**
 * 部门信息表前端操作接口
 * SubmissionFile frontend operation API
 **/
@RestController
@RequestMapping("/submissionFile")
public class SubmissionFileController {

    @Resource
    private SubmissionFileService submissionFileService;

    private static final Logger logger = Logger.getLogger(SubmissionFileController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增书签
     * Add a new submissionFile
     */
    @RequiresPermission("assignment:submit")
    @PostMapping("/add")
    public Result add(MultipartFile file, Integer submissionId) {
        logRequest("add", submissionId.toString());
        submissionFileService.add(file,submissionId);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除书签
     * Delete a submissionFile by ID
     */
    @RequiresPermission("assignment:submit")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        submissionFileService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除书签
     * Batch delete submissionFiles
     */
    @RequiresPermission("assignment:submit")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        submissionFileService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新书签
     * Update a submissionFile
     */
    @RequiresPermission("assignment:submit")
    @PutMapping("/update")
    public Result updateById(@RequestBody SubmissionFile submissionFile) {
        logRequest("updateById", submissionFile.toString());
        submissionFileService.updateById(submissionFile);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a submissionFile by ID
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        SubmissionFile submissionFile = submissionFileService.selectById(id);
        logResponse("selectById", submissionFile.toString());
        return Result.success(submissionFile);
    }

    /**
     * 查询所有书签
     * Query all submissionFiles
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectAll")
    public Result selectAll(SubmissionFile submissionFile) {
        logRequest("selectAll", submissionFile != null ? submissionFile.toString() : "null");
        List<SubmissionFile> list = submissionFileService.selectAll(submissionFile);
        logResponse("selectAll", null);
        return Result.success(list);
    }

}
