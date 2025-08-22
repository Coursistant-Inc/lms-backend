package com.coursistant.lms.controller.file;

import com.coursistant.lms.entity.FileSummary;
import com.coursistant.lms.service.file.DiskFilesService;
import com.coursistant.lms.service.file.SubmissionFileService;
import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.SubmissionFile;
import com.coursistant.lms.annotation.RequiresPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * SubmissionFile 前端操作接口
 * SubmissionFile frontend operation API
 */
@RestController
@RequestMapping("/submissionContentItem")
public class SubmissionFileController {

    @Resource
    private SubmissionFileService submissionFileService;

    @Resource
    private DiskFilesService diskFilesService;

    private static final Logger logger = Logger.getLogger(SubmissionFileController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @RequiresPermission("assignment:submit")
    @PostMapping("/add")
    public Result add(@RequestBody SubmissionFile submissionFile) {
        logRequest("add", submissionFile.toString());
        Integer submissionFileId = submissionFileService.add(submissionFile);
        logResponse("add", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("submissionFileId", submissionFileId);
        return Result.success(data);
    }

    @RequiresPermission("assignment:submit")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        submissionFileService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:submit")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        submissionFileService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:submit")
    @PutMapping("/update")
    public Result update(@RequestBody SubmissionFile submissionFile) {
        logRequest("update", submissionFile.toString());
        submissionFileService.updateById(submissionFile);
        logResponse("update", "Success");
        return Result.success();
    }

    @RequiresPermission("submission:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        SubmissionFile submissionFile = submissionFileService.selectById(id);
        logResponse("selectById", submissionFile.toString());
        return Result.success(submissionFile);
    }

    @RequiresPermission("submission:view")
    @GetMapping("/selectAll")
    public Result selectAll(SubmissionFile submissionFile) {
        logRequest("selectAll", submissionFile != null ? submissionFile.toString() : "null");
        List<SubmissionFile> list = submissionFileService.selectAll(submissionFile);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    @RequiresPermission("submission:view")
    @GetMapping("/selectBySubmissionId/{submissionId}")
    public Result selectBySubmissionId(@PathVariable Integer submissionId) {
        logRequest("selectBySubmissionId", submissionId.toString());
        List<SubmissionFile> list = submissionFileService.selectBySubmissionId(submissionId);
        logResponse("selectBySubmissionId", null);
        return Result.success(list);
    }

    @RequiresPermission("assignment:submit")
    @DeleteMapping("/deleteBySubmissionId/{submissionId}")
    public Result deleteBySubmissionId(@PathVariable Integer submissionId) {
        logRequest("deleteBySubmissionId", submissionId.toString());
        submissionFileService.deleteBySubmissionId(submissionId);
        logResponse("deleteBySubmissionId", "Success");
        return Result.success();
    }

    /**
     * Upload a file and create a SubmissionFile entry (type = file)
     */
    @RequiresPermission("assignment:submit")
    @PostMapping("/addWithFile")
    public Result addWithFile(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "submissionId", required = false) Integer submissionId,
                              @RequestParam("category") String category,
                              @RequestParam("courseId") Integer courseId,
                              @RequestParam("userId") Integer userId,
                              @RequestParam("orderIndex") Integer orderIndex) {
        logRequest("addWithFile", file.getOriginalFilename());

        FileSummary summary = diskFilesService.add(file, courseId, userId, category, 0);
        Integer fileId = summary.getId();

        SubmissionFile item = new SubmissionFile();
        item.setSubmissionId(submissionId);
        item.setType("file");
        item.setFileId(fileId);
        item.setUploadedBy(userId);
        item.setOrderIndex(orderIndex);

        Integer submissionFileId = submissionFileService.add(item);
        logResponse("addWithFile", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("submissionFileId", submissionFileId);
        return Result.success(data);
    }
}
