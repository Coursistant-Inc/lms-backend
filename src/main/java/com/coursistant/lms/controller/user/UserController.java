package com.coursistant.lms.controller.user;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.User;
import com.coursistant.lms.service.user.UserService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

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
    public Result add(@RequestBody User user) {
        logRequest("add", user.toString());
        userService.add(user);
        logResponse("add", user.toString());
        return Result.success();
    }

    /**
     * 删除
     * Delete a user by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        userService.deleteById(id);
        logResponse("deleteById", id.toString());
        return Result.success();
    }

    /**
     * 批量删除
     * Batch delete users
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        userService.deleteBatch(ids);
        logResponse("deleteBatch", ids.toString());
        return Result.success();
    }

    /**
     * 修改
     * Update a user
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody User user) {
        logRequest("updateById", user.toString());
        userService.updateById(user);
        logResponse("updateById", user.toString());
        return Result.success();
    }

    /**
     * 根据ID查询
     * Query a user by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        User user = userService.selectById(id);
        logResponse("selectById", user.toString());
        return Result.success(user);
    }

    /**
     * 查询所有
     * Query all users
     */
    @GetMapping("/selectAll")
    public Result selectAll(User user) {
        logRequest("selectAll", user.toString());
        List<User> list = userService.selectAll(user);
        logResponse("selectAll", null);
        return Result.success(list);
    }


    /**
     * 查询教师
     * Query all teachers
     */
    @GetMapping("/selectTeachers")
    public Result selectHeaders() {
        logRequest("selectTeachers", "request received");
        List<User> list = userService.selectTeachers();
        logResponse("selectTeachers", "null");
        return Result.success(list);
    }
}
