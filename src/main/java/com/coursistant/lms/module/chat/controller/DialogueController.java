package com.coursistant.lms.module.chat.controller;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.chat.entity.Dialogue;
import com.coursistant.lms.module.chat.service.DialogueService;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Logger;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 部门信息表前端操作接口
 * Dialogue frontend operation API
 **/
@RestController
@RequestMapping("/dialogue")
public class DialogueController {

    @Resource
    private DialogueService dialogueService;

    private static final Logger logger = Logger.getLogger(DialogueController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增对话
     * Add a new dialogue
     */
    @PostMapping("/add")
    public Result add(@RequestBody Dialogue dialogue,
                      @RequestHeader(value = "X-Timezone", required = true) String timezone) {
        logRequest("add", dialogue.toString());
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        dialogueService.add(dialogue, zone);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 软删除对话
     * Soft delete a dialogue
     */
    @PostMapping("/softDelete/{id}")
    public Result softDelete(@PathVariable Integer id) {
        logRequest("add", id.toString());
        dialogueService.softDelete(id);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除对话
     * Delete a dialogue by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        dialogueService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除对话
     * Batch delete dialogues
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        dialogueService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新对话
     * Update a dialogue
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Dialogue dialogue,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("updateById", dialogue.toString());
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        dialogueService.updateById(dialogue, zone);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询对话
     * Query a dialogue by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectById", id.toString());
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        Dialogue dialogue = dialogueService.selectById(id,zone);
        logResponse("selectById", dialogue.toString());
        return Result.success(dialogue);
    }

    /**
     * 根据用户 ID 查询对话
     * Query dialogues by user ID
     */
    @GetMapping("/selectByUserId/{id}")
    public Result selectByUserId(@PathVariable Integer id,
                                 @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectByUserId", id.toString());
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<Dialogue> list = dialogueService.selectByUserId(id,zone);
        logResponse("selectByUserId", null);
        return Result.success(list);
    }

    /**
     * 根据用户 ID 和关键字查询对话
     * Query dialogues by user ID and keyword
     */
    @GetMapping("/selectByUserIdAndKeyword")
    public Result selectByUserIdAndKeyword(@RequestParam("userId") Integer userId,
                                           @RequestParam(value = "keyword") String keyword,
                                           @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectByUserIdAndKeyword", "userId=" + userId + ", keyword=" + keyword);
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<Dialogue> list = dialogueService.selectByUserIdAndKeyword(userId, keyword,zone);
        logResponse("selectByUserIdAndKeyword", null);
        return Result.success(list);
    }

    /**
     * 查询所有对话
     * Query all dialogues
     */
    @GetMapping("/selectAll")
    public Result selectAll(Dialogue dialogue,
                            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectAll", dialogue != null ? dialogue.toString() : "null");
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<Dialogue> list = dialogueService.selectAll(dialogue,zone);
        logResponse("selectAll", null);
        return Result.success(list);
    }
}
