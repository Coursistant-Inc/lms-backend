package com.coursistant.lms.service.interaction;
import com.coursistant.lms.entity.Feedback;
import com.coursistant.lms.utils.TimeZoneUtils;
import org.springframework.stereotype.Service;
import com.coursistant.lms.mapper.interaction.FeedbackMapper;

import jakarta.annotation.Resource;
import java.time.ZoneId;
import java.util.List;


@Service
public class FeedbackService {

    @Resource
    private FeedbackMapper feedbackMapper;

    //private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 新增 Feedback
     * Add new Feedback
     */
    public void addFeedback(Feedback feedback) {
        feedbackMapper.insert(feedback);
    }

    /**
     * 根据 ID 删除 Feedback
     * Delete Feedback by ID
     */
    public void deleteById(Integer id) {
        feedbackMapper.deleteById(id);
    }

    /**
     * 批量删除 Feedback
     * Batch delete Feedback
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            feedbackMapper.deleteById(id);
        }
    }

    /**
     * 更新 Feedback
     * Update Feedback
     */
    public void updateById(Feedback feedback) {
        feedbackMapper.updateById(feedback);
    }

    /**
     * 根据 ID 查询 Feedback
     * Select Feedback by ID
     */
    public Feedback selectById(Integer id, ZoneId timezone) {
        Feedback feedback=feedbackMapper.selectById(id);
        feedback.setDate(TimeZoneUtils.fromUtcLocalDateTime(feedback.getDate(),timezone));
        return feedback;
    }

    /**
     * 查询所有 Feedback
     * Select all Feedback
     */
    public List<Feedback> selectAll(Feedback feedback1, ZoneId timezone) {
        List<Feedback> feedbacks=feedbackMapper.selectAll(feedback1);
        for (Feedback feedback:feedbacks){
            feedback.setDate(TimeZoneUtils.fromUtcLocalDateTime(feedback.getDate(),timezone));
        }
        return feedbacks;
    }

    /**
     * 查询某个用户的所有 Feedback
     * Select all Feedback from a specific user
     */
    public List<Feedback> selectByUserId(Integer userId, ZoneId timezone) {

        List<Feedback> feedbacks=feedbackMapper.selectByUserId(userId);
        for (Feedback feedback:feedbacks){
            feedback.setDate(TimeZoneUtils.fromUtcLocalDateTime(feedback.getDate(),timezone));
        }
        return feedbacks;
    }
}
