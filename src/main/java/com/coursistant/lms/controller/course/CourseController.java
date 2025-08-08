package com.coursistant.lms.controller.course;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Course;
import com.coursistant.lms.service.course.CourseService;
import com.coursistant.lms.annotation.RequiresPermission;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * 部门信息表前端操作接口
 * Course frontend operation API
 **/
@RestController
@RequestMapping("/course")
public class CourseController {

    @Resource
    private CourseService courseService;

    private static final Logger logger = Logger.getLogger(CourseController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增
     * Add a new course
     */
    @RequiresPermission("course:manage")
    @PostMapping("/add")
    public Result add(@RequestBody Course course) {
        logRequest("add", course.toString());
        Integer courseId = courseService.add(course);
        logResponse("add", course.toString());
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", courseId);
        return Result.success(data);
    }

    /**
     * 删除
     * Delete a course by ID
     */
    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        courseService.deleteById(id);
        logResponse("deleteById", id.toString());
        return Result.success();
    }

    /**
     * 批量删除
     * Batch delete courses
     */
    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        courseService.deleteBatch(ids);
        logResponse("deleteBatch", ids.toString());
        return Result.success();
    }

    /**
     * 修改
     * Update a course
     */
    @RequiresPermission("course:manage")
    @PutMapping("/update")
    public Result updateById(@RequestBody Course course) {
        logRequest("updateById", course.toString());
        courseService.updateById(course);
        logResponse("updateById", course.toString());
        return Result.success();
    }

    /**
     * 根据ID查询
     * Query a course by ID
     */
    @RequiresPermission("course:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Course course = courseService.selectById(id);
        logResponse("selectById", course.toString());
        return Result.success(course);
    }

    /**
     * 查询所有
     * Query all courses
     */
    @RequiresPermission("course:view")
    @GetMapping("/selectAll")
    public Result selectAll(Course course) {
        logRequest("selectAll", course != null ? course.toString() : "null");
        List<Course> list = courseService.selectAll(course);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    /**
     * 查询所有
     * Query all courses by userId
     */
    @RequiresPermission("course:view")
    @GetMapping("/selectByUserId/{id}")
    public Result selectByUserId(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        List<Course> list = courseService.selectByUserId(id);
        logResponse("selectById", null);
        return Result.success(list);
    }

}
