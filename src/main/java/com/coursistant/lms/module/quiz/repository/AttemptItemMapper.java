package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.AttemptItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

public interface AttemptItemMapper {

    /**
     * 新增
     * Insert a new attemptItem record
     */
    int insert(AttemptItem attemptItem);

    // 批量新增 AttemptItem
    int insertBatch(@Param("items") List<AttemptItem> items);

    /**
     * 删除
     * Delete a attemptItem record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a attemptItem record by ID
     */
    int updateById(AttemptItem attemptItem);

    /**
     * 根据 ID 查询
     * Query a attemptItem record by ID
     */
    AttemptItem selectById(Integer id);

    /**
     * 查询所有（可带条件）
     * Query all attemptItem records (filterable)
     */
    List<AttemptItem> selectAll(AttemptItem filter);

    @Select("SELECT IFNULL(SUM(final_score), 0) FROM AttemptItem WHERE attempt_id = #{attemptId}")
    int selectTotalFinalScoreByAttemptId(Integer attemptId);

}
