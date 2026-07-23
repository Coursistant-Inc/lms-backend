package com.coursistant.lms.module.auth.admin.controller;

import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.service.AdminService;
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
    public ApiResponse<Void> add(@RequestBody Admin admin) {
        logRequest("add", admin.toString());
        adminService.add(admin);
        logResponse("add", admin.toString());
        return ApiResponse.success();
    }

    /**
     * 删除
     * Delete by ID
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        adminService.deleteById(id);
        logResponse("deleteById", id.toString());
        return ApiResponse.success();
    }

    /**
     * 批量删除
     * Batch delete
     */
    @DeleteMapping("/delete/batch")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        adminService.deleteBatch(ids);
        logResponse("deleteBatch", ids.toString());
        return ApiResponse.success();
    }

    /**
     * 修改
     * Update admin details
     */
    @PutMapping("/update")
    public ApiResponse<Void> updateById(@RequestBody Admin admin) {
        logRequest("updateById", admin.toString());
        adminService.updateById(admin);
        logResponse("updateById", admin.toString());
        return ApiResponse.success();
    }

    /**
     * 根据ID查询
     * Query by ID
     */
    @GetMapping("/selectById/{id}")
    public ApiResponse<Admin> selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Admin admin = adminService.selectById(id);
        logResponse("selectById", admin.toString());
        return ApiResponse.success(admin);
    }

    /**
     * 查询所有
     * Query all admins
     */
    @GetMapping("/selectAll")
    public ApiResponse<List<Admin>> selectAll(Admin admin) {
        logRequest("selectAll", admin.toString());
        List<Admin> list = adminService.selectAll(admin);
        logResponse("selectAll", null);
        return ApiResponse.success(list);
    }


}
