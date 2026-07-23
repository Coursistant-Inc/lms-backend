package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.quiz.entity.Question;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.entity.QuizItem;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.quiz.repository.QuizItemMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

@Service
public class QuizItemService {

    @Resource
    private QuizItemMapper quizItemMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private QuestionService questionService;

    public void add(QuizItem quizItem) {
        quizItemMapper.insert(quizItem);
    }


    // ======= 新增方法：根据 quizId, questionId, points, orderIndex 生成 QuizItem 并入库 =======
    public void addQuestion(Integer quizId, Integer questionId, java.math.BigDecimal points, Integer orderIndex) {
        // 1) 查题目
        Question q = questionService.selectById(questionId);
        if (q == null) {
            // 你项目里的异常/枚举按需替换
            throw new CustomException(ResultCodeEnum.GROUP_NOT_EXIST_ERROR);
        }

        // 2) 组装 question_snapshot（最小够用：type/stem/choices/answerKey）
        String snapshotStr;
        try {
            ObjectNode snapshot = objectMapper.createObjectNode();
            snapshot.put("type", q.getType());
            snapshot.put("stem", q.getStem());

            if (q.getChoices() != null) {
                JsonNode choicesNode = objectMapper.readTree(q.getChoices()); // JSON 数组
                snapshot.set("choices", choicesNode);
            }
            if (q.getAnswerKey() != null) {
                JsonNode answerKeyNode = objectMapper.readTree(q.getAnswerKey()); // JSON 对象
                snapshot.set("answerKey", answerKeyNode);
            }

            snapshotStr = objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build question_snapshot", e);
        }

        // 3) 构造 QuizItem 并入库
        QuizItem quizItem = new QuizItem();
        quizItem.setQuizId(quizId);
        quizItem.setQuestionId(questionId);
        quizItem.setPoints(points);
        quizItem.setOrderIndex(orderIndex);
        quizItem.setQuestionSnapshot(snapshotStr);

        quizItemMapper.insert(quizItem);
    }


    /**
     * 删除
     * Delete a quiz item by ID
     */
    public void deleteById(Integer id) {
        quizItemMapper.deleteById(id);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 批量删除
     * Delete multiple quiz items by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            quizItemMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update a quiz item by ID
     */
    public void updateById(QuizItem quizItem) {
        quizItemMapper.updateById(quizItem);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 根据ID查询
     * Query a quiz item by ID
     */
    public QuizItem selectById(Integer id) {
        QuizItem item = quizItemMapper.selectById(id);
        if (item == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }
        return item;
    }

    /**
     * 查询所有
     * Query all quiz items
     */
    public List<QuizItem> selectAll(QuizItem filter) {
        return quizItemMapper.selectAll(filter);
    }


    /**
     * 根据 questionId 查询所有 QuizItem
     * Get all quiz items by questionId
     */
    public List<QuizItem> selectByQuestionId(Integer questionId) {
        return quizItemMapper.selectByQuestionId(questionId);
    }

    /**
     * 根据 quizId 查询所有 QuizItem
     */
    public List<QuizItem> selectByQuizId(Integer quizId) {
        return quizItemMapper.selectByQuizId(quizId);
    }


}