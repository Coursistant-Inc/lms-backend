package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.quiz.repository.AttemptItemMapper;
import com.coursistant.lms.module.quiz.repository.QuizAttemptMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

@Service
public class QuizAttemptService {

    @Resource
    private QuizAttemptMapper quizAttemptMapper;

    @Resource
    private AttemptItemMapper attemptItemMapper;

    public Integer add(QuizAttempt attempt) {
        quizAttemptMapper.insert(attempt);

        return attempt.getId();
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

    public int updateGrade(Integer attemptId) {
        // 1) 计算总分
        int grade = attemptItemMapper.selectTotalFinalScoreByAttemptId(attemptId);

        // 2) 更新 QuizAttempt.grade
        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(attemptId);
        attempt.setFinalScore(BigDecimal.valueOf(grade));
        quizAttemptMapper.updateById(attempt);

        return grade;
    }

    public QuizAttemptService(QuizAttemptMapper quizAttemptMapper) {
        this.quizAttemptMapper = quizAttemptMapper;
    }

    /**
     * 【功能1】按 quizId 查询“去重后的最新尝试列表”
     *
     * 业务含义：
     * - 给定 quizId，查询该测验下所有尝试记录
     * - 按 student_id 去重（同一个学生可能有多次尝试）
     * - 每个 student_id 只保留一条：主键 id 最大（通常代表最新插入的一次尝试）
     *
     * 返回：
     * - List<QuizAttempt>：每个学生一条最新记录；若 quizId 为空则返回空列表
     */
    public List<QuizAttempt> getLatestAttemptsDistinctStudentByQuizId(Integer quizId) {
        if (quizId == null) {
            return Collections.emptyList();
        }
        return quizAttemptMapper.selectLatestByQuizIdDistinctStudent(quizId);
    }

    /**
     * 【功能2】按 quizId + studentId 查询“该学生最新一次尝试”
     *
     * 业务含义：
     * - 给定 quizId 和 studentId
     * - 返回该学生在该测验下主键 id 最大的一条记录（最新尝试）
     *
     * 返回：
     * - QuizAttempt：存在则返回最新记录；若参数为空或查不到则返回 null
     */
    public QuizAttempt getLatestAttemptByQuizIdAndStudentId(Integer quizId, Integer studentId) {
        if (quizId == null || studentId == null) {
            return null;
        }
        return quizAttemptMapper.selectLatestByQuizIdAndStudentId(quizId, studentId);
    }

}