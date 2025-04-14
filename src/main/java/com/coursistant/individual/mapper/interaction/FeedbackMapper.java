package com.coursistant.individual.mapper.interaction;
import com.coursistant.individual.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 操作 Feedback 相关数据接口
 * Data access interface for Feedback-related operations
 */
@Mapper
public interface FeedbackMapper {

    /**
     * 新增反馈
     * Insert a new Feedback record
     */
    int insert(Feedback feedback);

    /**
     * 根据 ID 删除反馈
     * Delete a Feedback record by ID
     */
    int deleteById(Integer id);

    /**
     * 更新反馈
     * Update a Feedback record by ID
     */
    int updateById(Feedback feedback);

    /**
     * 根据 ID 查询反馈
     * Query a Feedback record by ID
     */
    Feedback selectById(Integer id);

    /**
     * 查询所有反馈
     * Query all Feedback records
     */
    List<Feedback> selectAll();

    /**
     * 查询某个用户的所有反馈
     * Query all Feedback records for a specific user
     */
    @Select("SELECT * FROM Feedback WHERE user_id = #{userId}")
    List<Feedback> selectByUserId(Integer userId);
}
