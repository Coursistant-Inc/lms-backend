package com.coursistant.lms.module.chat.repository;

import com.coursistant.lms.module.chat.entity.Chat;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 操作teach相关数据接口
 * Data access interface for chat-related operations
 */
public interface ChatMapper {

    /**
     * 新增
     * Insert a new chat record
     */
    int insert(Chat chat);

    /**
     * 删除
     * Delete a chat record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a chat record by ID
     */
    int updateById(Chat chat);

    /**
     * 根据ID查询
     * Query a chat record by ID
     */
    Chat selectById(Integer id);

    /**
     * 查询所有
     * Query all chat records
     */
    List<Chat> selectAll(Chat chat);

    @Select("select * from Chat where dialogue_id = #{dialogueId}")
    List<Chat> selectByDialogueId(Integer dialogueId);

    int updateSoftDeleteByDialogueId(@Param("dialogueId") Integer dialogueId,
                                     @Param("delete") int delete,
                                     @Param("deleteTime") LocalDateTime deleteTime);


    @Select("SELECT * FROM Chat WHERE dialogue_id = #{dialogueId} ORDER BY id DESC LIMIT 5")
    List<Chat> selectTop5ByDialogueId(Integer dialogueId);

}