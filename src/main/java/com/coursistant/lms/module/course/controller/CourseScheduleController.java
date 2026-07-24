package com.coursistant.lms.module.course.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.course.entity.CourseSchedule;
import com.coursistant.lms.module.course.service.CourseScheduleService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 课程排课前端操作接口
 * Course schedule frontend operation API
 */
@RestController
@RequestMapping("/courseSchedule")
public class CourseScheduleController {

    @Resource
    private CourseScheduleService courseScheduleService;

    private static final Logger logger = Logger.getLogger(CourseScheduleController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增排课
     * Add a new course schedule
     */
    @PostMapping("/add")
    public Result add(@RequestBody CourseSchedule courseSchedule) {
        logRequest("add", courseSchedule.toString());
        courseScheduleService.add(courseSchedule);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 删除排课记录
     * Delete a schedule by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        courseScheduleService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除排课记录
     * Batch delete schedules
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        courseScheduleService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 修改排课记录
     * Update a course schedule
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody CourseSchedule courseSchedule) {
        logRequest("updateById", courseSchedule.toString());
        courseScheduleService.updateById(courseSchedule);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询排课记录
     * Query a schedule by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        CourseSchedule schedule = courseScheduleService.selectById(id);
        logResponse("selectById", schedule.toString());
        return Result.success(schedule);
    }

    /**
     * 查询所有排课记录
     * Query all schedules
     */
    @GetMapping("/selectAll")
    public Result selectAll(CourseSchedule condition) {
        logRequest("selectAll", condition != null ? condition.toString() : "null");
        List<CourseSchedule> list = courseScheduleService.selectAll(condition);
        logResponse("selectAll", null);
        return Result.success(list);
    }
}
