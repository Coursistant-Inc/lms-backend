package com.coursistant.lms.service.chat;


import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Chat;
import com.coursistant.lms.mapper.chat.ChatMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
public class ChatService {

    @Resource
    private ChatMapper chatMapper;


    public void add(Chat chat) {
        chatMapper.insert(chat);
    }


    public void softDelete(Integer id) {
        // 搜索 / Search
        Chat chat = chatMapper.selectById(id);
        if (chat == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }
        // 修改 / Modify
        chat.setDelete(true);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        chat.setDeleteTime(LocalDateTime.now().format(formatter));
        // 更新 / Update
        chatMapper.updateById(chat);
    }

    /**
     * 删除
     * Delete a chat by ID
     */
    public void deleteById(Integer id) {
        chatMapper.deleteById(id);
    }

    /**
     * 批量删除
     * Delete multiple chats by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            chatMapper.deleteById(id);
        }

    }

    /**
     * 修改
     * Update a chat by ID
     */
    public void updateById(Chat chat) {
        chatMapper.updateById(chat);
    }

    /**
     * 根据ID查询
     * Query a chat by ID
     */
    public Chat selectById(Integer id) {

        Chat chat = chatMapper.selectById(id);
        if (chat == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }

        return chat;
    }

    /**
     * 查询所有
     * Query all chats
     */
    public List<Chat> selectAll(Chat chat) {

        List<Chat> chats = chatMapper.selectAll(chat);

        return chats;
    }

    /**
     * 根据对话 ID 查询
     * Query chats by dialogue ID
     */
    public List<Chat> selectByDialogueId(Integer id) {

        List<Chat> chats = chatMapper.selectByDialogueId(id);

        return chats;
    }

    /**
     * 根据对话 ID 软删除
     * Soft delete chats by dialogue ID
     */
    public void updateSoftDeleteByDialogueId(Integer dialogueId) {
        int rowsUpdated = chatMapper.updateSoftDeleteByDialogueId(dialogueId, 1,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    //return last 5 chat
    public List<Chat> getTop5ChatsByDialogueId(Integer dialogueId) {

        return chatMapper.selectTop5ByDialogueId(dialogueId);
    }


}
