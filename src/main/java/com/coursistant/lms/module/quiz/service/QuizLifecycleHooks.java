package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.repository.QuizAttemptMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Best-effort finalize of InProgress attempts when course/membership changes.
 * Scheduler remains the backstop (~5s).
 */
@Service
public class QuizLifecycleHooks {

    private static final Logger log = LoggerFactory.getLogger(QuizLifecycleHooks.class);
    private static final int BATCH = 200;

    @Resource
    private QuizAttemptMapper quizAttemptMapper;
    @Resource
    private QuizFinalizeService quizFinalizeService;

    public void onCourseArchived(Integer courseId) {
        if (courseId == null) {
            return;
        }
        List<Integer> ids = quizAttemptMapper.selectInProgressIdsByCourseId(courseId, BATCH);
        for (Integer id : ids) {
            safeFinalize(id, QuizConstants.CLOSE_COURSE_ARCHIVED);
        }
    }

    public void onMembershipIneligible(Integer courseId, Integer userId) {
        if (courseId == null || userId == null) {
            return;
        }
        List<Integer> ids = quizAttemptMapper.selectInProgressIdsByCourseIdAndUserId(courseId, userId, BATCH);
        for (Integer id : ids) {
            safeFinalize(id, QuizConstants.CLOSE_MEMBERSHIP_INELIGIBLE);
        }
    }

    private void safeFinalize(Integer attemptId, String reason) {
        try {
            quizFinalizeService.finalizeAttempt(attemptId, reason);
        } catch (Exception e) {
            log.warn("Quiz finalize hook failed: attemptId={}, reason={}, err={}",
                    attemptId, reason, e.toString());
        }
    }
}
