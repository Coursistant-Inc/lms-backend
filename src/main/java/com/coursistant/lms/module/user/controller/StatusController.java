package com.coursistant.lms.module.user.controller;

import com.coursistant.lms.module.user.service.StatusService;
import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.user.entity.Status;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 部门信息表前端操作接口
 * Status frontend operation API
 **/
@RestController
@RequestMapping("/status")
public class StatusController {

    @Resource
    private StatusService statusService;

    private static final Logger logger = Logger.getLogger(StatusController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增书签
     * Add a new status
     */
    @PostMapping("/add")
    public Result add(@RequestBody Status status) {
        logRequest("add", status.toString());
        statusService.add(status);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除书签
     * Delete a status by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        statusService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除书签
     * Batch delete statuss
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        statusService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新书签
     * Update a status
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Status status) {
        logRequest("updateById", status.toString());
        statusService.updateById(status);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询书签
     * Query a status by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Status status = statusService.selectById(id);
        logResponse("selectById", status.toString());
        return Result.success(status);
    }

    /**
     * 查询所有书签
     * Query all statuss
     */
    @GetMapping("/selectAll")
    public Result selectAll(Status status) {
        logRequest("selectAll", status != null ? status.toString() : "null");
        List<Status> list = statusService.selectAll(status);
        logResponse("selectAll", null);
        return Result.success(list);
    }

}
