package com.coursistant.lms.controller.interaction;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Feedback;
import com.coursistant.lms.service.interaction.FeedbackService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;



/**
 * Feedback 反馈前端操作接口
 * Feedback frontend operation API
 **/
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Resource
    private FeedbackService feedbackService;

    /**
     * 新增反馈
     * Add new feedback
     */
    @PostMapping("/add")
    public Result add(@RequestBody Feedback feedback) {
        feedbackService.addFeedback(feedback);
        return Result.success();
    }

    /**
     * 根据 ID 删除反馈
     * Delete feedback by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        feedbackService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除反馈
     * Batch delete feedback
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        feedbackService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 更新反馈
     * Update feedback
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Feedback feedback) {
        feedbackService.updateById(feedback);
        return Result.success();
    }

    /**
     * 根据 ID 查询反馈
     * Query feedback by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Feedback feedback = feedbackService.selectById(id);
        return Result.success(feedback);
    }

    /**
     * 查询所有反馈
     * Query all feedback
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        List<Feedback> list = feedbackService.selectAll();
        return Result.success(list);
    }

    /**
     * 查询某个用户的所有反馈
     * Query all feedback from a specific user
     */
    @GetMapping("/selectByUserId/{userId}")
    public Result selectByUserId(@PathVariable Integer userId) {
        List<Feedback> list = feedbackService.selectByUserId(userId);
        return Result.success(list);
    }
}
