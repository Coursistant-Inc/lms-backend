package com.coursistant.lms.mapper.quiz;

import com.coursistant.lms.entity.Question;

import java.util.List;

public interface QuestionMapper {

    /**
     * 新增
     * Insert a new question record
     */
    int insert(Question question);

    /**
     * 删除
     * Delete a question record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a question record by ID
     */
    int updateById(Question question);

    /**
     * 根据 ID 查询
     * Query a question record by ID
     */
    Question selectById(Integer id);

    /**
     * 查询所有（可带条件）
     * Query all question records (filterable)
     */
    List<Question> selectAll(Question filter);
}
