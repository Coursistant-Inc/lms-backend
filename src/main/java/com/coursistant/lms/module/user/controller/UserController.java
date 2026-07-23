package com.coursistant.lms.module.user.controller;

import java.util.List;
import java.util.logging.Logger;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * User frontend operation API
 **/
@RestController
@RequestMapping("/v1/users")
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

    @PostMapping
    public ApiResponse<Void> add(@RequestBody User user) {
        logRequest("add", user.toString());
        userService.add(user);
        logResponse("add", user.toString());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        userService.deleteById(id);
        logResponse("deleteById", id.toString());
        return ApiResponse.success();
    }

    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        userService.deleteBatch(ids);
        logResponse("deleteBatch", ids.toString());
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateById(@PathVariable Integer id, @RequestBody User user) {
        user.setId(id);
        logRequest("updateById", user.toString());
        userService.updateById(user);
        logResponse("updateById", user.toString());
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<User> selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        User user = userService.selectById(id);
        logResponse("selectById", user.toString());
        return ApiResponse.success(user);
    }

    /**
     * List users. Pass role=teacher to return teachers only.
     */
    @GetMapping
    public ApiResponse<List<User>> selectAll(
            User user,
            @RequestParam(value = "role", required = false) String role) {
        if ("teacher".equalsIgnoreCase(role)) {
            logRequest("selectTeachers", "role=teacher");
            List<User> list = userService.selectTeachers();
            logResponse("selectTeachers", "null");
            return ApiResponse.success(list);
        }
        logRequest("selectAll", user != null ? user.toString() : "null");
        List<User> list = userService.selectAll(user);
        logResponse("selectAll", null);
        return ApiResponse.success(list);
    }

    @PostMapping("/{userId}/name-change-requests")
    public ApiResponse<String> nameChangeRequest(
            @PathVariable Integer userId,
            @RequestParam("currentName") String currentName,
            @RequestParam("newName") String newName) {
        userService.updateName(currentName, newName, userId);
        return ApiResponse.success("Your request has been received. You will be notified once a decision has been taken");
    }

    @PostMapping("/{userId}/name-change-requests/review")
    public ApiResponse<Void> reviewNameChangeRequest(
            @PathVariable Integer userId,
            @RequestParam("decision") String decision,
            @RequestParam("adminId") Integer adminId) {
        userService.reviewNameChangeRequest(decision, userId, adminId);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/password-status")
    public ApiResponse<Void> markPasswordChanged(@PathVariable Integer id) {
        logRequest("markPasswordChanged", id.toString());
        userService.markPasswordChanged(id);
        logResponse("markPasswordChanged", "User " + id + " must_change_password set to false");
        return ApiResponse.success();
    }
}
