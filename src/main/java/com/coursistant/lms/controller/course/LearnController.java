package com.coursistant.lms.controller.course;

import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Learn;
import com.coursistant.lms.entity.User;
import com.coursistant.lms.service.course.LearnService;
import com.coursistant.lms.annotation.RequiresPermission;

import cn.hutool.core.util.ObjectUtil;

/**
 * 部门信息表前端操作接口
 * Learn frontend operation API
 **/
@RestController
@RequestMapping("/learn")
public class LearnController {

    @Resource
    private LearnService learnService;

    private static final Logger logger = Logger.getLogger(LearnController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增
     * Add a new learning record
     */
    @RequiresPermission("course:manage")
    @PostMapping("/add")
    public Result add(@RequestBody Learn learn) {
        logRequest("add", learn.toString());
        learnService.add(learn);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 通过电子邮件新增学习记录
     * Add a learning record by email
     */
    @RequiresPermission("course:manage")
    @PostMapping("/addByEmail")
    public Result addByEmail(@RequestParam("email") String email,
                             @RequestParam("courseId") Integer courseId,
                             @RequestParam(value = "file", required = false) MultipartFile file) {
        logRequest("addByEmail", email);
        if (ObjectUtil.isEmpty(email) || ObjectUtil.isEmpty(courseId)) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        learnService.addByEmail(email, courseId, file);
        logResponse("addByEmail", "Success");
        return Result.success();
    }

    /**
     * 删除
     * Delete a learning record by ID
     */
    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        learnService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除
     * Batch delete learning records
     */
    @RequiresPermission("course:manage")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        learnService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 修改
     * Update a learning record
     */
    @RequiresPermission("course:manage")
    @PutMapping("/update")
    public Result updateById(@RequestBody Learn learn) {
        logRequest("updateById", learn.toString());
        learnService.updateById(learn);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据ID查询
     * Query a learning record by ID
     */
    @RequiresPermission("course:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Learn learn = learnService.selectById(id);
        logResponse("selectById", learn.toString());
        return Result.success(learn);
    }

    /**
     * 查询所有
     * Query all learning records
     */
    @RequiresPermission("course:view")
    @GetMapping("/selectAll")
    public Result selectAll(Learn learn) {
        logRequest("selectAll", learn != null ? learn.toString() : "null");
        List<Learn> list = learnService.selectAll(learn);
        logResponse("selectAll", null);
        return Result.success(list);
    }

    /**
     * 分页查询
     * Paginated query for learning records
     */



    @PostMapping("/update/courseStatus")
    public Result updateCourseStatus(@RequestParam(value="user_id") Integer userId, @RequestParam("course_id") Integer courseId, @RequestParam("course_status") String courseStatus)
    {

        learnService.updateCourseStatus(userId, courseId, courseStatus);
        // return "Update successful!";
        return Result.success();
    }

    @GetMapping("/select/courseStatus")
    public Result selectCourseStatus(@RequestParam(value="user_id") Integer userId, @RequestParam(value="course_id") Integer courseId) {
        
        String courseStatus = learnService.selectCourseStatus(userId, courseId);
        return Result.success(courseStatus);
    }

    @PostMapping("/update/grade")
    public Result updateCourseGrade(@RequestParam(value="user_id") Integer userId, @RequestParam(value="course_id") Integer courseId, @RequestParam(value="grade") String grade) {
        //TODO: process POST request
        
        learnService.updateCourseGrade(userId, courseId, grade);
        return Result.success();
    }

    @GetMapping("/select/grade")
    public Result selectCourseGrade(@RequestParam(value="user_id") Integer userId, @RequestParam(value="course_id") Integer courseId) {
        String grade = learnService.selectCourseGrade(userId, courseId);
        // return grade;
        return Result.success(grade);
    }
    
    
    @GetMapping("/selectByCourseId/{id}")
    public Result selectByCourseId(@PathVariable Integer id) {
        logRequest("selectByCourseId", id.toString());
        List<User> students = learnService.getStudentsByCourseId(id);
        logResponse("selectByCourseId", null);
        return Result.success(students);
    }

    
}
