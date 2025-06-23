package com.coursistant.lms.service.chat;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Chat;
import com.coursistant.lms.entity.Dialogue;
import com.coursistant.lms.mapper.chat.DialogueMapper;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.sql.Time;
import java.time.ZoneId;

@Service
public class DialogueService {

    @Resource
    private DialogueMapper dialogueMapper;
    @Resource
    private ChatService chatService;

    /**
     * 添加对话
     * Add a new dialogue
     */
    public void add(Dialogue dialogue, ZoneId timezone) {
        dialogue.setUpdateTime(TimeZoneUtils.toUtcLocalDateTime(dialogue.getUpdateTime(),timezone));
        dialogue.setDeleteTime(TimeZoneUtils.toUtcLocalDateTime(dialogue.getDeleteTime(),timezone));
        dialogueMapper.insert(dialogue);

    }

    /**
     * 软删除对话
     * Soft delete a dialogue
     */
    public void softDelete(Integer id, ZoneId timezone) {
        // 查询对话 / Search dialogue
        Dialogue dialogue = dialogueMapper.selectById(id);
        if (dialogue == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }
        // 修改删除状态 / Modify delete status
        dialogue.setDelete(true);
        dialogue.setDeleteTime(TimeZoneUtils.toUtcLocalDateTime(LocalDateTime.now(),timezone));
        // 更新对话状态 / Update dialogue status
        dialogueMapper.updateById(dialogue);
        chatService.updateSoftDeleteByDialogueId(dialogue.getId(), timezone);
    }

    /**
     * 删除对话
     * Delete a dialogue
     */
    public void deleteById(Integer id) {
        // 查询对话 / Search dialogue
        Dialogue dialogue = dialogueMapper.selectById(id);
        if (dialogue == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }
        dialogueMapper.deleteById(id);
    }

    /**
     * 批量删除对话
     * Batch delete dialogues
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 更新对话
     * Update dialogue
     */
    public void updateById(Dialogue dialogue, ZoneId timezone) {
        dialogue.setUpdateTime(TimeZoneUtils.toUtcLocalDateTime(dialogue.getUpdateTime(),timezone));
        dialogue.setDeleteTime(TimeZoneUtils.toUtcLocalDateTime(dialogue.getDeleteTime(),timezone));
        dialogueMapper.updateById(dialogue);
    }

    /**
     * 根据 ID 查询对话
     * Select dialogue by ID
     */
    public Dialogue selectById(Integer id, ZoneId timezone) {
        Dialogue dialogue = dialogueMapper.selectById(id);
        if (dialogue == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }
        dialogue.setUpdateTime(TimeZoneUtils.fromUtcLocalDateTime(dialogue.getUpdateTime(),timezone));
        dialogue.setDeleteTime(TimeZoneUtils.fromUtcLocalDateTime(dialogue.getDeleteTime(),timezone));

        List<Chat> chats = chatService.selectByDialogueId(dialogue.getId());
        dialogue.setChats(chats);
        return dialogue;
    }

    /**
     * 根据用户 ID 查询对话
     * Select dialogues by user ID
     */
    public List<Dialogue> selectByUserId(Integer id, ZoneId timezone) {

        List<Dialogue> dialogues = dialogueMapper.selectByUserId(id);
        if (!ObjectUtil.isNotNull(dialogues)){
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }

        for (int i=0;i<dialogues.size();i++){
            Dialogue dialogue = dialogues.get(i);
            dialogue.setUpdateTime(TimeZoneUtils.fromUtcLocalDateTime(dialogue.getUpdateTime(),timezone));
            dialogue.setDeleteTime(TimeZoneUtils.fromUtcLocalDateTime(dialogue.getDeleteTime(),timezone));

            Integer singleid=dialogue.getId();
            List<Chat> chats=chatService.selectByDialogueId(singleid);
            dialogues.get(i).setChats(chats);
        }

        return dialogues;
    }

    /**
     * 根据用户 ID 和关键词查询对话
     * Select dialogues by user ID and keyword
     */
    public List<Dialogue> selectByUserIdAndKeyword(Integer userId, String keyword, ZoneId timezone) {

        List<Dialogue> dialogues = dialogueMapper.selectByUserIdAndKeyword(userId, keyword);
        for (Dialogue d : dialogues) {
            d.setUpdateTime(TimeZoneUtils.fromUtcLocalDateTime(d.getUpdateTime(), timezone));
            d.setDeleteTime(TimeZoneUtils.fromUtcLocalDateTime(d.getDeleteTime(), timezone));
        }
        return dialogues;
    }

    /**
     * 查询所有对话
     * Select all dialogues
     */
    public List<Dialogue> selectAll(Dialogue dialogue, ZoneId timezone) {

        List<Dialogue> dialogues = dialogueMapper.selectAll(dialogue);
        for (Dialogue d : dialogues) {
            d.setUpdateTime(TimeZoneUtils.fromUtcLocalDateTime(d.getUpdateTime(), timezone));
            d.setDeleteTime(TimeZoneUtils.fromUtcLocalDateTime(d.getDeleteTime(), timezone));
        }
        return dialogues;
    }

    /**
     * 获取当前最大 ID
     * Get the current maximum ID
     */
    public Integer getMaxId() {
        Integer id=dialogueMapper.selectMaxId();
        if (id==null){
            id=0;
        }

        return  id;
    }
}
