package com.coursistant.lms.controller.course;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.DTO.FolderDTO;
import com.coursistant.lms.entity.Folder;
import com.coursistant.lms.service.course.CourseContentService;
import com.coursistant.lms.annotation.RequiresPermission;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;

/**
 * Folder 前端操作接口
 * Folder frontend operation API
 */
@RestController
@RequestMapping("/courseContent")
public class CourseContentController {

    @Resource
    private CourseContentService courseContentService;

    private static final Logger logger = Logger.getLogger(CourseContentController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    @RequiresPermission("course:manage")
    @PostMapping("/add")
    public Result add(@RequestBody Folder folder) {
        logRequest("add", folder.toString());
        Integer courseContentId = courseContentService.add(folder);
        logResponse("add", "Success");
        Map<String, Object> data = new HashMap<>();
        data.put("courseContentId", courseContentId);
        return Result.success(data);
    }

    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        courseContentService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        courseContentService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    @RequiresPermission("course:manage")
    @PutMapping("/update")
    public Result update(@RequestBody Folder folder) {
        logRequest("update", folder.toString());
        courseContentService.updateById(folder);
        logResponse("update", "Success");
        return Result.success();
    }

    @RequiresPermission("course:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Folder folder = courseContentService.selectById(id);
        logResponse("selectById", folder.toString());
        return Result.success(folder);
    }

    @RequiresPermission("course:view")
    @GetMapping("/selectAll")
    public Result selectAll(Folder folder) {
        logRequest("selectAll", folder != null ? folder.toString() : "null");
        List<Folder> list = courseContentService.selectAll(folder);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    @RequiresPermission("course:view")
    @GetMapping("/selectByCourseIdWithItems/{courseId}")
    public Result selectByCourseIdWithItems(@PathVariable Integer courseId) {
        logRequest("selectByCourseIdWithItems", courseId.toString());
        List<FolderDTO> list = courseContentService.getFoldersWithItemsByCourseId(courseId);
        logResponse("selectByCourseIdWithItems", null);
        return Result.success(list);
    }

}