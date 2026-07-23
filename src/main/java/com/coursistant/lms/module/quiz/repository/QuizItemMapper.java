package com.coursistant.lms.module.quiz.repository;

import com.coursistant.lms.module.quiz.entity.QuizItem;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

public interface QuizItemMapper {

    /**
     * 新增
     * Insert a new quizItem record
     */
    int insert(QuizItem quizItem);

    /**
     * 删除
     * Delete a quizItem record by ID
     */
    int deleteById(Integer id);

    /**
     * 修改
     * Update a quizItem record by ID
     */
    int updateById(QuizItem quizItem);

    /**
     * 根据 ID 查询
     * Query a quizItem record by ID
     */
    QuizItem selectById(Integer id);

    /**
     * 查询所有（可带条件）
     * Query all quizItem records (filterable)
     */
    List<QuizItem> selectAll(QuizItem filter);

    // 新增：根据 questionId 查询全部 QuizItem
    List<QuizItem> selectByQuestionId(Integer questionId);

    // 新增：根据 quizId 查询全部 QuizItem
    List<QuizItem> selectByQuizId(Integer quizId);


}
