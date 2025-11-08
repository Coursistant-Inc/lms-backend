package com.coursistant.lms.service.quiz;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Question;
import com.coursistant.lms.mapper.quiz.QuestionMapper;
import com.coursistant.lms.exception.CustomException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    @Resource
    private QuestionMapper questionMapper;

    public void add(Question question) {
        questionMapper.insert(question);
    }

    /**
     * 删除
     * Delete a question by ID
     */
    public void deleteById(Integer id) {
        questionMapper.deleteById(id);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 批量删除
     * Delete multiple questions by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            questionMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update a question by ID
     */
    public void updateById(Question question) {
        questionMapper.updateById(question);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 根据ID查询
     * Query a question by ID
     */
    public Question selectById(Integer id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }
        return question;
    }

    /**
     * 查询所有
     * Query all questions
     */
    public List<Question> selectAll(Question filter) {
        return questionMapper.selectAll(filter);
    }
}
