package com.coursistant.lms.module.chat.repository;

import com.coursistant.lms.module.chat.entity.Dialogue;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * 操作 Dialogue 相关数据接口
 * Data access interface for Dialogue-related operations
 */
public interface DialogueMapper {

    /**
     * 新增
     * Insert a new Dialogue record
     */
    int insert(Dialogue dialogue);

    /**
     * 删除
     * Delete a Dialogue record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a Dialogue record by ID
     */
    int updateById(Dialogue dialogue);

    /**
     * 根据 ID 查询
     * Query a Dialogue record by ID
     */
    Dialogue selectById(Integer id);

    /**
     * 查询所有
     * Query all Dialogue records
     */
    List<Dialogue> selectAll(Dialogue dialogue);

    /**
     * 获取当前最大 ID
     * Get the current maximum ID
     */
    Integer selectMaxId();

    /**
     * 根据用户 ID 查询
     * Query Dialogues by user ID
     */
    @Select("select * from Dialogue where user_id = #{userId}")
    List<Dialogue> selectByUserId(Integer userId);

    /**
     * 根据用户 ID 和关键字查询
     * Query Dialogues by user ID and keyword
     */
    List<Dialogue> selectByUserIdAndKeyword(Integer userId, String keyword);
}
