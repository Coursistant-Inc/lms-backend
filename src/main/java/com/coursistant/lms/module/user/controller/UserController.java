package com.coursistant.lms.module.user.controller;

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

import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.module.user.entity.User;
import com.coursistant.lms.module.user.service.UserService;

/**
 * 用户前端操作接口
 * User frontend operation API
 **/
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增
     * Add a new user
     */
    @PostMapping("/add")
    public ApiResponse<Void> add(@RequestBody User user) {
        logRequest("add", user.toString());
        userService.add(user);
        logResponse("add", user.toString());
        return ApiResponse.success();
    }

    /**
     * 删除
     * Delete a user by ID
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        userService.deleteById(id);
        logResponse("deleteById", id.toString());
        return ApiResponse.success();
    }

    /**
     * 批量删除
     * Batch delete users
     */
    @DeleteMapping("/delete/batch")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        userService.deleteBatch(ids);
        logResponse("deleteBatch", ids.toString());
        return ApiResponse.success();
    }

    /**
     * 修改
     * Update a user
     */
    @PutMapping("/update")
    public ApiResponse<Void> updateById(@RequestBody User user) {
        logRequest("updateById", user.toString());
        userService.updateById(user);
        logResponse("updateById", user.toString());
        return ApiResponse.success();
    }

    /**
     * 根据ID查询
     * Query a user by ID
     */
    @GetMapping("/selectById/{id}")
    public ApiResponse<User> selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        User user = userService.selectById(id);
        logResponse("selectById", user.toString());
        return ApiResponse.success(user);
    }

    /**
     * 查询所有
     * Query all users
     */
    @GetMapping("/selectAll")
    public ApiResponse<List<User>> selectAll(User user) {
        logRequest("selectAll", user.toString());
        List<User> list = userService.selectAll(user);
        logResponse("selectAll", null);
        return ApiResponse.success(list);
    }


    /**
     * 查询教师
     * Query all teachers
     */
    @GetMapping("/selectTeachers")
    public ApiResponse<List<User>> selectHeaders() {
        logRequest("selectTeachers", "request received");
        List<User> list = userService.selectTeachers();
        logResponse("selectTeachers", "null");
        return ApiResponse.success(list);
    }

    @PostMapping("/nameChange")
    public ApiResponse<String> nameChangeRequest(@RequestParam("currentName") String currentName, @RequestParam("newName") String newName, @RequestParam("userId") Integer userId)
    {
        userService.updateName(currentName, newName, userId);
        return ApiResponse.success("Your request has been received. You will be notified once a decision has been taken");
    }

    // This method should be accessible only to university admins
    @PostMapping("/reviewNameChange")
    public void reviewNameChangeRequest(@RequestParam("decision") String decision, @RequestParam("userId") Integer userId, @RequestParam("adminId") Integer adminId)
    {
        userService.reviewNameChangeRequest(decision, userId, adminId);
    }

    /**
     * 标记用户已修改密码
     * Mark user's must_change_password as false
     */
    @PutMapping("/markPasswordChanged/{id}")
    public ApiResponse<Void> markPasswordChanged(@PathVariable Integer id) {
        logRequest("markPasswordChanged", id.toString());
        userService.markPasswordChanged(id);
        logResponse("markPasswordChanged", "User " + id + " must_change_password set to false");
        return ApiResponse.success();
    }
}
