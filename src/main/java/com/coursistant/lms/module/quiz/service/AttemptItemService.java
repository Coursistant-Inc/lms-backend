package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.quiz.entity.AttemptItem;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.quiz.repository.AttemptItemMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.coursistant.lms.module.chat.entity.Query;

@Service
public class AttemptItemService {

    @Resource
    private AttemptItemMapper attemptItemMapper;

    public void add(AttemptItem item) {
        attemptItemMapper.insert(item);
    }

    // AttemptItemService.java


    @Transactional
    public void addBatch(List<AttemptItem> items) {
        if (items == null || items.isEmpty()) {
            return; // 或抛参数异常
        }
        // 可选：同一次批量必须属于同一个 attemptId
        Integer attemptId0 = items.get(0).getAttemptId();
        for (AttemptItem it : items) {
            if (!attemptId0.equals(it.getAttemptId())) {
                throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
            }
        }
        attemptItemMapper.insertBatch(items);
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