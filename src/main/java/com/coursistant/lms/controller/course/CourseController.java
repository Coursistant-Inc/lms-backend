package com.coursistant.lms.controller.course;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Course;
import com.coursistant.lms.service.course.CourseService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
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
    @PostMapping("/add")
    public Result add(@RequestBody Course course) {
        logRequest("add", course.toString());
        courseService.add(course);
        logResponse("add", course.toString());
        return Result.success();
    }

    /**
     * 删除
     * Delete a course by ID
     */
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
    @GetMapping("/selectByUserId/{id}")
    public Result selectByUserId(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        List<Course> list = courseService.selectByUserId(id);
        logResponse("selectById", null);
        return Result.success(list);
    }

}
