package com.coursistant.lms.mapper.quiz;

import com.coursistant.lms.entity.QuizAttempt;

import java.util.List;

public interface QuizAttemptMapper {

    /**
     * 新增
     * Insert a new quizAttempt record
     */
    int insert(QuizAttempt quizAttempt);

    /**
     * 删除
     * Delete a quizAttempt record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a quizAttempt record by ID
     */
    int updateById(QuizAttempt quizAttempt);

    /**
     * 根据 ID 查询
     * Query a quizAttempt record by ID
     */
    QuizAttempt selectById(Integer id);

    /**
     * 查询所有（可带条件）
     * Query all quizAttempt records (filterable)
     */
    List<QuizAttempt> selectAll(QuizAttempt filter);
}
