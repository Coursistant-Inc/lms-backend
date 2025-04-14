package com.coursistant.individual.controller.chat;

import com.coursistant.individual.common.Result;
import com.coursistant.individual.entity.Chat;
import com.coursistant.individual.service.chat.ChatService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

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
    public Result add(@RequestBody Chat chat) {
        logRequest("add", chat.toString());
        chatService.add(chat);
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
    public Result updateById(@RequestBody Chat chat) {
        logRequest("updateById", chat.toString());
        chatService.updateById(chat);
        logResponse("updateById", "Success");
        return Result.success();
    }

    /**
     * 根据 ID 查询聊天记录
     * Query a chat record by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        logRequest("selectById", id.toString());
        Chat chat = chatService.selectById(id);
        logResponse("selectById", chat.toString());
        return Result.success(chat);
    }

    /**
     * 查询所有聊天记录
     * Query all chat records
     */
    @GetMapping("/selectAll")
    public Result selectAll(Chat chat) {
        logRequest("selectAll", chat != null ? chat.toString() : "null");
        List<Chat> list = chatService.selectAll(chat);
        logResponse("selectAll", null);
        return Result.success(list);
    }
}
