package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.quiz.entity.Question;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.quiz.repository.QuestionMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

@Service
public class QuizService {

    @Resource
    private QuizMapper quizMapper;

    public Integer add(Quiz quiz) {
        quizMapper.insert(quiz);
        return quiz.getId();
    }


    /**
     * 删除
     * Delete a quiz by ID
     */
    public void deleteById(Integer id) {
        quizMapper.deleteById(id);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 批量删除
     * Delete multiple quizzes by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            quizMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update a quiz by ID
     */
    public void updateById(Quiz quiz) {
        quizMapper.updateById(quiz);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 根据ID查询
     * Query a quiz by ID
     */
    public Quiz selectById(Integer id) {
        Quiz quiz = quizMapper.selectById(id);
        if (quiz == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }
        return quiz;
    }

    /**
     * 查询所有
     * Query all quizzes
     */
    public List<Quiz> selectAll(Quiz filter) {
        return quizMapper.selectAll(filter);
    }
}
