package com.coursistant.lms.module.course.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.course.entity.Teach;
import com.coursistant.lms.module.course.service.TeachService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 部门信息表前端操作接口
 * Teach frontend operation API
 **/
@RestController
@RequestMapping("/teach")
public class TeachController {

    @Resource
    private TeachService teachService;

    private static final Logger logger = Logger.getLogger(TeachController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增
     * Add a new teaching record
     */
    @PostMapping("/add")
    public Result add(@RequestBody Teach teach) {
        logRequest("add", teach.toString());
        teachService.add(teach);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 删除
     * Delete a teaching record by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        teachService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除
     * Batch delete teaching records
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        teachService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 修改
     * Update a teaching record
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Teach teach) {
        logRequest("updateById", teach.toString());
        teachService.updateById(teach);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据ID查询
     * Query a teaching record by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Teach teach = teachService.selectById(id);
        logResponse("selectById", teach.toString());
        return Result.success(teach);
    }

    /**
     * 查询所有
     * Query all teaching records
     */
    @GetMapping("/selectAll")
    public Result selectAll(Teach teach) {
        logRequest("selectAll", teach != null ? teach.toString() : "null");
        List<Teach> list = teachService.selectAll(teach);
        logResponse("selectAll", null);
        return Result.success(list);
    }


}
