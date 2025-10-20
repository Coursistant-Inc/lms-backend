package com.coursistant.lms.service.quiz;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.AttemptItem;
import com.coursistant.lms.entity.Quiz;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.quiz.AttemptItemMapper;
import com.coursistant.lms.mapper.quiz.QuizMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttemptItemService {

    @Resource
    private AttemptItemMapper attemptItemMapper;

    public void add(AttemptItem item) {
        attemptItemMapper.insert(item);
    }

    /**
     * 删除
     * Delete an attempt item by ID
     */
    public void deleteById(Integer id) {
        attemptItemMapper.deleteById(id);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 批量删除
     * Delete multiple attempt items by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            attemptItemMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update an attempt item by ID
     */
    public void updateById(AttemptItem item) {
        attemptItemMapper.updateById(item);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 根据ID查询
     * Query an attempt item by ID
     */
    public AttemptItem selectById(Integer id) {
        AttemptItem res = attemptItemMapper.selectById(id);
        if (res == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }
        return res;
    }

    /**
     * 查询所有
     * Query all attempt items
     */
    public List<AttemptItem> selectAll(AttemptItem filter) {
        return attemptItemMapper.selectAll(filter);
    }
}