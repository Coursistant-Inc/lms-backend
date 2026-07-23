package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.file.entity.DiskFiles;
import com.coursistant.lms.module.file.entity.FileSummary;
import com.coursistant.lms.module.assignment.entity.AssignmentItem;
import com.coursistant.lms.module.assignment.service.AssignmentContentItemService;
import com.coursistant.lms.module.course.service.CourseContentItemService;
import com.coursistant.lms.module.file.service.DiskFilesService;
import com.coursistant.lms.shared.security.RequiresPermission;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * AssignmentItem 前端操作接口
 * AssignmentItem frontend operation API
 */
@RestController
@RequestMapping("/assignmentContentItem")
public class AssignmentContentItemController {

    @Resource
    private AssignmentContentItemService assignmentContentItemService;

    @Resource
    private DiskFilesService diskFilesService;

    private static final Logger logger = Logger.getLogger(AssignmentContentItemController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @RequiresPermission("assignment:manage")
    @PostMapping("/add")
    public Result add(@RequestBody AssignmentItem assignmentItem) {
        logRequest("add", assignmentItem.toString());
        Integer courseContentItemId= assignmentContentItemService.add(assignmentItem);
        logResponse("add", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("courseContentItemId", courseContentItemId);
        return Result.success(data);
    }

    @RequiresPermission("assignment:manage")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        assignmentContentItemService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:manage")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        assignmentContentItemService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:manage")
    @PutMapping("/update")
    public Result update(@RequestBody AssignmentItem assignmentItem) {
        logRequest("update", assignmentItem.toString());
        assignmentContentItemService.updateById(assignmentItem);
        logResponse("update", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        AssignmentItem assignmentItem = assignmentContentItemService.selectById(id);
        logResponse("selectById", assignmentItem.toString());
        return Result.success(assignmentItem);
    }

    @RequiresPermission("assignment:view")
    @GetMapping("/selectAll")
    public Result selectAll(AssignmentItem assignmentItem) {
        logRequest("selectAll", assignmentItem != null ? assignmentItem.toString() : "null");
        List<AssignmentItem> list = assignmentContentItemService.selectAll(assignmentItem);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    @RequiresPermission("assignment:view")
    @GetMapping("/selectByAssignmentId/{assignmentId}")
    public Result selectByAssignmentId(@PathVariable Integer assignmentId) {
        logRequest("selectByAssignmentId", assignmentId.toString());
        List<AssignmentItem> list = assignmentContentItemService.selectByAssignmentId(assignmentId);
        logResponse("selectByAssignmentId", null);
        return Result.success(list);
    }

    @RequiresPermission("assignment:manage")
    @DeleteMapping("/deleteByAssignmentId/{assignmentId}")
    public Result deleteByAssignmentId(@PathVariable Integer assignmentId) {
        logRequest("deleteByAssignmentId", assignmentId.toString());
        assignmentContentItemService.deleteByAssignmentId(assignmentId);
        logResponse("deleteByAssignmentId", "Success");
        return Result.success();
    }

    @RequiresPermission("assignment:view")
    @GetMapping("/selectCourseInfo/{courseId}")
    public Result selectCourseInfo(@PathVariable Integer courseId) {
        logRequest("selectCourseInfo", courseId.toString());
        List<AssignmentItem> list = assignmentContentItemService.selectCourseInfo(courseId);
        logResponse("selectCourseInfo", null);
        return Result.success(list);
    }

    /**
     * Upload a file and create a AssignmentItem entry (type = file)
     */
    @RequiresPermission("assignment:manage")
    @PostMapping("/addWithFile")
    public Result addWithFile(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "assignmentId", required = false) Integer assignmentId,
                              @RequestParam("category") String category,
                              @RequestParam("courseId") Integer courseId,
                              @RequestParam("userId") Integer userId,
                              @RequestParam("analysis") Integer analysis,
                              @RequestParam("orderIndex") Integer orderIndex) {
        logRequest("addWithFile", file.getOriginalFilename());

        FileSummary summary = diskFilesService.add(file, courseId, userId, category, analysis,true);
        Integer fileId = summary.getId();

        AssignmentItem item = new AssignmentItem();
        item.setAssignmentId(assignmentId);
        item.setType("file");
        item.setFileId(fileId);
        item.setUploadedBy(userId);
        item.setOrderIndex(orderIndex);

        Integer courseContentItemId= assignmentContentItemService.add(item);
        logResponse("addWithFile", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("assignmentContentItemId", courseContentItemId);
        return Result.success(data);
    }

    /**
     * 根据课程ID查询作业相关文件
     * Query assignment-related files by courseId
     */
    @RequiresPermission("assignment:submit")
    @GetMapping("/selectAssignmentFilesByCourseId")
    public Result selectAssignmentFilesByCourseId(@RequestParam Integer courseId) {
        logRequest("selectAssignmentFilesByCourseId", "courseId=" + courseId);
        List<DiskFiles> files = assignmentContentItemService.selectAssignmentFilesByCourseId(courseId);
        logResponse("selectAssignmentFilesByCourseId", "count=" + files.size());
        return Result.success(files);
    }


}