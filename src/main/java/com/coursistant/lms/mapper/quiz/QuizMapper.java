package com.coursistant.lms.mapper.quiz;

import com.coursistant.lms.entity.Quiz;

import java.util.List;

public interface QuizMapper {

    /**
     * 新增
     * Insert a new quiz record
     */
    int insert(Quiz quiz);

    /**
     * 删除
     * Delete a quiz record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a quiz record by ID
     */
    int updateById(Quiz quiz);

    /**
     * 根据 ID 查询
     * Query a quiz record by ID
     */
    Quiz selectById(Integer id);

    /**
     * 查询所有（可带条件）
     * Query all quiz records (filterable)
     */
    List<Quiz> selectAll(Quiz filter);
}
