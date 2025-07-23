package com.coursistant.lms.controller.system;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Admin;
import com.coursistant.lms.service.system.AdminService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

/**
 * 管理员前端操作接口
 * Admin frontend operation API
 **/
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s ", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增
     * Add new admin
     */
    @PostMapping("/add")
    public Result add(@RequestBody Admin admin) {
        logRequest("add", admin.toString());
        adminService.add(admin);
        logResponse("add", admin.toString());
        return Result.success();
    }

    /**
     * 删除
     * Delete by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        adminService.deleteById(id);
        logResponse("deleteById", id.toString());
        return Result.success();
    }

    /**
     * 批量删除
     * Batch delete
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        adminService.deleteBatch(ids);
        logResponse("deleteBatch", ids.toString());
        return Result.success();
    }

    /**
     * 修改
     * Update admin details
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Admin admin) {
        logRequest("updateById", admin.toString());
        adminService.updateById(admin);
        logResponse("updateById", admin.toString());
        return Result.success();
    }

    /**
     * 根据ID查询
     * Query by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Admin admin = adminService.selectById(id);
        logResponse("selectById", admin.toString());
        return Result.success(admin);
    }

    /**
     * 查询所有
     * Query all admins
     */
    @GetMapping("/selectAll")
    public Result selectAll(Admin admin) {
        logRequest("selectAll", admin.toString());
        List<Admin> list = adminService.selectAll(admin);
        logResponse("selectAll", null);
        return Result.success(list);
    }


}
