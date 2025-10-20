package com.coursistant.lms.service.quiz;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Quiz;
import com.coursistant.lms.entity.QuizAttempt;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.quiz.QuizAttemptMapper;
import com.coursistant.lms.mapper.quiz.QuizMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizAttemptService {

    @Resource
    private QuizAttemptMapper quizAttemptMapper;

    public void add(QuizAttempt attempt) {
        quizAttemptMapper.insert(attempt);
    }

    /**
     * 删除
     * Delete a quiz attempt by ID
     */
    public void deleteById(Integer id) {
        quizAttemptMapper.deleteById(id);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 批量删除
     * Delete multiple quiz attempts by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            quizAttemptMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update a quiz attempt by ID
     */
    public void updateById(QuizAttempt attempt) {
        quizAttemptMapper.updateById(attempt);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 根据ID查询
     * Query a quiz attempt by ID
     */
    public QuizAttempt selectById(Integer id) {
        QuizAttempt res = quizAttemptMapper.selectById(id);
        if (res == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }
        return res;
    }

    /**
     * 查询所有
     * Query all quiz attempts
     */
    public List<QuizAttempt> selectAll(QuizAttempt filter) {
        return quizAttemptMapper.selectAll(filter);
    }
}