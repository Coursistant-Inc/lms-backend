package com.coursistant.lms.module.quiz.job;

import com.coursistant.lms.module.quiz.service.QuizAttemptService;
import com.coursistant.lms.module.quiz.service.QuizFinalizeService;
import com.coursistant.lms.module.quiz.service.QuizTimeSupport;
import com.coursistant.lms.module.quiz.repository.QuizAttemptMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizFinalizeScheduler {

    private static final int BATCH_SIZE = 100;

    @Resource
    private QuizAttemptMapper quizAttemptMapper;
    @Resource
    private QuizMapper quizMapper;
    @Resource
    private QuizFinalizeService quizFinalizeService;
    @Resource
    private QuizAttemptService quizAttemptService;
    @Resource
    private QuizTimeSupport quizTimeSupport;

    @Scheduled(fixedDelay = 5000)
    public void scanAndFinalize() {
        List<Integer> ids = quizAttemptMapper.selectIdsNeedingFinalize(quizTimeSupport.nowUtc(), BATCH_SIZE);
        for (Integer attemptId : ids) {
            QuizAttempt attempt = quizAttemptMapper.selectById(attemptId);
            if (attempt == null) {
                continue;
            }
            Quiz quiz = quizMapper.selectById(attempt.getQuizId());
            String reason = quizAttemptService.resolveCloseReason(attempt, quiz, quiz.getCourseId());
            quizFinalizeService.finalizeAttempt(attemptId, reason);
        }
    }
}
