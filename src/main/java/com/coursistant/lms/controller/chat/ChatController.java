package com.coursistant.lms.controller.chat;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Chat;
import com.coursistant.lms.service.chat.ChatService;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;
import java.time.ZoneId;
/**
 * 部门信息表前端操作接口
 * Chat frontend operation API
 **/
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    private static final Logger logger = Logger.getLogger(ChatController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * 新增聊天记录
     * Add a new chat record
     */
    @PostMapping("/add")
    public Result add(@RequestBody Chat chat,
                      @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("add", chat.toString());
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        chatService.add(chat, zone);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 软删除聊天记录
     * Soft delete a chat record
     */
    @PostMapping("/softDelete/{id}")
    public Result softDelete(@PathVariable Integer id) {
        logRequest("add", id.toString());
        chatService.softDelete(id);
        logResponse("add", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 删除聊天记录
     * Delete a chat record by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        logRequest("deleteById", id.toString());
        chatService.deleteById(id);
        logResponse("deleteById", "Success");
        return Result.success();
    }

    /**
     * 批量删除聊天记录
     * Batch delete chat records
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        logRequest("deleteBatch", ids.toString());
        chatService.deleteBatch(ids);
        logResponse("deleteBatch", "Success");
        return Result.success();
    }

    /**
     * 更新聊天记录
     * Update a chat record
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Chat chat,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("updateById", chat.toString());
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        chatService.updateById(chat, zone);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询聊天记录
     * Query a chat record by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id,
                             @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectById", id.toString());
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        Chat chat = chatService.selectById(id,zone);
        logResponse("selectById", chat.toString());
        return Result.success(chat);
    }

    /**
     * 查询所有聊天记录
     * Query all chat records
     */
    @GetMapping("/selectAll")
    public Result selectAll(Chat chat,
                            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        logRequest("selectAll", chat != null ? chat.toString() : "null");
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<Chat> list = chatService.selectAll(chat, zone);
        logResponse("selectAll", null);
        return Result.success(list);
    }
}
