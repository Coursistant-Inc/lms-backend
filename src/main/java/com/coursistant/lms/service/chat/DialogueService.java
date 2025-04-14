package com.coursistant.lms.service.chat;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Chat;
import com.coursistant.lms.entity.Dialogue;
import com.coursistant.lms.mapper.chat.DialogueMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    public void add(Dialogue dialogue) {
        dialogueMapper.insert(dialogue);
    }

    /**
     * 软删除对话
     * Soft delete a dialogue
     */
    public void softDelete(Integer id) {
        // 查询对话 / Search dialogue
        Dialogue dialogue = dialogueMapper.selectById(id);
        if (dialogue == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }
        // 修改删除状态 / Modify delete status
        dialogue.setDelete(true);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dialogue.setDeleteTime(LocalDateTime.now().format(formatter));
        // 更新对话状态 / Update dialogue status
        dialogueMapper.updateById(dialogue);
        chatService.updateSoftDeleteByDialogueId(dialogue.getId());
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
    public void updateById(Dialogue dialogue) {
        dialogueMapper.updateById(dialogue);
    }

    /**
     * 根据 ID 查询对话
     * Select dialogue by ID
     */
    public Dialogue selectById(Integer id) {
        Dialogue dialogue = dialogueMapper.selectById(id);
        if (dialogue == null) {
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }
        List<Chat> chats = chatService.selectByDialogueId(dialogue.getId());
        dialogue.setChats(chats);
        return dialogue;
    }

    /**
     * 根据用户 ID 查询对话
     * Select dialogues by user ID
     */
    public List<Dialogue> selectByUserId(Integer id) {

        List<Dialogue> dialogues = dialogueMapper.selectByUserId(id);
        if (!ObjectUtil.isNotNull(dialogues)){
            throw new CustomException(ResultCodeEnum.CHAT_NOT_EXIST_ERROR);
        }

        for (int i=0;i<dialogues.size();i++){
            Integer singleid=dialogues.get(i).getId();
            List<Chat> chats=chatService.selectByDialogueId(singleid);
            dialogues.get(i).setChats(chats);
        }

        return dialogues;
    }

    /**
     * 根据用户 ID 和关键词查询对话
     * Select dialogues by user ID and keyword
     */
    public List<Dialogue> selectByUserIdAndKeyword(Integer userId, String keyword) {

        List<Dialogue> dialogues = dialogueMapper.selectByUserIdAndKeyword(userId, keyword);
        return dialogues;
    }

    /**
     * 查询所有对话
     * Select all dialogues
     */
    public List<Dialogue> selectAll(Dialogue dialogue) {

        List<Dialogue> dialogues = dialogueMapper.selectAll(dialogue);

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
