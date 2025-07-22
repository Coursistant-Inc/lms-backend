package com.coursistant.lms.service.chat;


import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Chat;
import com.coursistant.lms.mapper.chat.ChatMapper;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.List;


@Service
public class ChatService {

    @Resource
    private ChatMapper chatMapper;


    public void add(Chat chat, ZoneId timezone) {
        if (chat.getTime() != null) {
            chat.setTime(TimeZoneUtils.toUtcLocalDateTime(chat.getTime(), timezone));
        }
        if (chat.getDeleteTime() != null) {
            chat.setDeleteTime(TimeZoneUtils.toUtcLocalDateTime(chat.getDeleteTime(), timezone));
        }
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
        chat.setDeleteTime(TimeZoneUtils.toUtcLocalDateTime(LocalDateTime.now(), ZoneId.systemDefault()));
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
    public void updateById(Chat chat, ZoneId timezone) {
        if (chat.getTime() != null) {
            chat.setTime(TimeZoneUtils.toUtcLocalDateTime(chat.getTime(), timezone));
        }
        if (chat.getDeleteTime() != null) {
            chat.setDeleteTime(TimeZoneUtils.toUtcLocalDateTime(chat.getDeleteTime(), timezone));
        }
        chatMapper.updateById(chat);
    }

    /**
     * 根据ID查询
     * Query a chat by ID
     */
    public Chat selectById(Integer id, ZoneId timezone) {

        Chat chat = chatMapper.selectById(id);
        if (chat == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }
        if (chat.getTime() != null) {
            chat.setTime(TimeZoneUtils.fromUtcLocalDateTime(chat.getTime(), timezone));
        }
        if (chat.getDeleteTime() != null) {
            chat.setDeleteTime(TimeZoneUtils.fromUtcLocalDateTime(chat.getDeleteTime(), timezone));
        }
        return chat;
    }

    /**
     * 查询所有
     * Query all chats
     */
    public List<Chat> selectAll(Chat chat, ZoneId timezone) {

        List<Chat> chats = chatMapper.selectAll(chat);
        for (Chat singleChat : chats) {
            if (singleChat.getTime() != null) {
                singleChat.setTime(TimeZoneUtils.fromUtcLocalDateTime(singleChat.getTime(), timezone));
            }
            if (singleChat.getDeleteTime() != null) {
                singleChat.setDeleteTime(TimeZoneUtils.fromUtcLocalDateTime(singleChat.getDeleteTime(), timezone));
            }
        }
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
                TimeZoneUtils.toUtcLocalDateTime(LocalDateTime.now(), ZoneId.systemDefault()));
    }

    //return last 5 chat
    public List<Chat> getTop5ChatsByDialogueId(Integer dialogueId) {

        return chatMapper.selectTop5ByDialogueId(dialogueId);
    }


}
