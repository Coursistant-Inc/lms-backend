package com.coursistant.lms.module.interaction.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.interaction.entity.Feedback;
import com.coursistant.lms.module.interaction.service.FeedbackService;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.ZoneId;
import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;



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
    public Result selectById(@PathVariable Integer id,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        ZoneId zone= TimeZoneUtils.resolveZoneId(timezone);
        Feedback feedback = feedbackService.selectById(id,zone);
        return Result.success(feedback);
    }

    /**
     * 查询所有反馈
     * Query all feedback
     */
    @GetMapping("/selectAll")
    public Result selectAll(Feedback feedback,
                            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        ZoneId zone= TimeZoneUtils.resolveZoneId(timezone);
        List<Feedback> list = feedbackService.selectAll(feedback,zone);
        return Result.success(list);
    }

    /**
     * 查询某个用户的所有反馈
     * Query all feedback from a specific user
     */
    @GetMapping("/selectByUserId/{userId}")
    public Result selectByUserId(@PathVariable Integer userId,
                                 @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        ZoneId zone= TimeZoneUtils.resolveZoneId(timezone);
        List<Feedback> list = feedbackService.selectByUserId(userId,zone);
        return Result.success(list);
    }
}
