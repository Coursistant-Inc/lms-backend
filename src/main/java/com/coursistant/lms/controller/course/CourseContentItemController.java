package com.coursistant.lms.controller.course;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.FileSummary;
import com.coursistant.lms.entity.FolderItem;
import com.coursistant.lms.service.file.DiskFilesService;
import com.coursistant.lms.service.course.CourseContentItemService;
import com.coursistant.lms.annotation.RequiresPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;

/**
 * FolderItem 前端操作接口
 * FolderItem frontend operation API
 */
@RestController
@RequestMapping("/courseContentItem")
public class CourseContentItemController {

    @Resource
    private CourseContentItemService courseContentItemService;

    @Resource
    private DiskFilesService diskFilesService;

    private static final Logger logger = Logger.getLogger(CourseContentItemController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @RequiresPermission("course:manage")
    @PostMapping("/add")
    public Result add(@RequestBody FolderItem folderItem) {
        logRequest("add", folderItem.toString());
        Integer courseContentItemId= courseContentItemService.add(folderItem);
        logResponse("add", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("courseContentItemId", courseContentItemId);
        return Result.success(data);
    }

    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        courseContentItemService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        courseContentItemService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @RequiresPermission("course:manage")
    @PutMapping("/update")
    public Result update(@RequestBody FolderItem folderItem) {
        logRequest("update", folderItem.toString());
        courseContentItemService.updateById(folderItem);
        logResponse("update", "Success");
        return Result.success();
    }

    @RequiresPermission("course:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        FolderItem folderItem = courseContentItemService.selectById(id);
        logResponse("selectById", folderItem.toString());
        return Result.success(folderItem);
    }

    @RequiresPermission("course:view")
    @GetMapping("/selectAll")
    public Result selectAll(FolderItem folderItem) {
        logRequest("selectAll", folderItem != null ? folderItem.toString() : "null");
        List<FolderItem> list = courseContentItemService.selectAll(folderItem);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    @RequiresPermission("course:view")
    @GetMapping("/selectByFolderId/{folderId}")
    public Result selectByFolderId(@PathVariable Integer folderId) {
        logRequest("selectByFolderId", folderId.toString());
        List<FolderItem> list = courseContentItemService.selectByFolderId(folderId);
        logResponse("selectByFolderId", null);
        return Result.success(list);
    }

    @RequiresPermission("course:manage")
    @DeleteMapping("/deleteByFolderId/{folderId}")
    public Result deleteByFolderId(@PathVariable Integer folderId) {
        logRequest("deleteByFolderId", folderId.toString());
        courseContentItemService.deleteByFolderId(folderId);
        logResponse("deleteByFolderId", "Success");
        return Result.success();
    }

    @RequiresPermission("course:view")
    @GetMapping("/selectCourseInfo/{courseId}")
    public Result selectCourseInfo(@PathVariable Integer courseId) {
        logRequest("selectCourseInfo", courseId.toString());
        List<FolderItem> list = courseContentItemService.selectCourseInfo(courseId);
        logResponse("selectCourseInfo", null);
        return Result.success(list);
    }

    /**
     * Upload a file and create a FolderItem entry (type = file)
     */
    @RequiresPermission("course:manage")
    @PostMapping("/addWithFile")
    public Result addWithFile(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "folderId", required = false) Integer folderId,
                              @RequestParam("category") String category,
                              @RequestParam("courseId") Integer courseId,
                              @RequestParam("userId") Integer userId,
                              @RequestParam("analysis") Integer analysis,
                              @RequestParam("isCourseInfo") Integer isCourseInfo,
                              @RequestParam("orderIndex") Integer orderIndex) {
        logRequest("addWithFile", file.getOriginalFilename());

        FileSummary summary = diskFilesService.add(file, courseId, userId, category, analysis);
        Integer fileId = summary.getId();

        FolderItem item = new FolderItem();
        item.setCourseId(courseId);
        item.setFolderId(folderId);
        item.setType("file");
        item.setFileId(fileId);
        item.setUploadedBy(userId);
        item.setIsCourseInfo(isCourseInfo);
        item.setOrderIndex(orderIndex);

        Integer courseContentItemId= courseContentItemService.add(item);
        logResponse("addWithFile", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("courseContentItemId", courseContentItemId);
        return Result.success(data);
    }

}