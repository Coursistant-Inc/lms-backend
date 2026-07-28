package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.dto.attempt.AutosaveRequest;
import com.coursistant.lms.module.quiz.dto.attempt.AutosaveResponse;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import com.coursistant.lms.module.quiz.entity.QuizAttemptAnswer;
import com.coursistant.lms.module.quiz.entity.QuizQuestion;
import com.coursistant.lms.module.quiz.repository.QuizAttemptAnswerMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.quiz.repository.QuizQuestionMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizAutosaveService {

    @Resource
    private QuizMapper quizMapper;
    @Resource
    private QuizQuestionMapper quizQuestionMapper;
    @Resource
    private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    @Resource
    private QuizAccessService quizAccessService;
    @Resource
    private QuizAttemptService quizAttemptService;
    @Resource
    private QuizScoringService quizScoringService;
    @Resource
    private QuizJsonUtil quizJsonUtil;
    @Resource
    private QuizTimeSupport quizTimeSupport;

    @Transactional
    public AutosaveResponse autosave(Integer courseId, Integer quizId, Integer attemptId,
                                     Integer questionId, Integer userId, AutosaveRequest body) {
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        if (quiz == null) {
            throw new ApiException(ErrorType.QUIZ_NOT_FOUND);
        }
        QuizAttempt attempt = quizAttemptService.requireOwnedInProgress(courseId, quizId, attemptId, userId);
        if (!QuizConstants.ATTEMPT_IN_PROGRESS.equals(attempt.getStatus())) {
            throw new ApiException(ErrorType.QUIZ_ATTEMPT_NOT_IN_PROGRESS);
        }
        QuizQuestion question = quizQuestionMapper.selectByQuizIdAndId(quizId, questionId);
        if (question == null) {
            throw new ApiException(ErrorType.QUIZ_QUESTION_NOT_FOUND);
        }
        quizScoringService.validateAnswerPayload(question.getType(),
                body == null ? null : body.getSelectedOptionIds(),
                body == null ? null : body.getTextAnswer());

        var now = quizTimeSupport.nowUtc();
        QuizAttemptAnswer answer = new QuizAttemptAnswer();
        answer.setAttemptId(attemptId);
        answer.setQuestionId(questionId);
        answer.setSelectedOptionIdsJson(quizJsonUtil.toOptionIdsJson(body == null ? null : body.getSelectedOptionIds()));
        answer.setTextAnswer(body == null ? null : trim(body.getTextAnswer(), 10000));
        answer.setSavedAt(now);
        answer.setCreatedAt(now);
        answer.setUpdatedAt(now);
        quizAttemptAnswerMapper.upsert(answer);

        QuizAttemptAnswer saved = quizAttemptAnswerMapper.selectByAttemptIdAndQuestionId(attemptId, questionId);
        AutosaveResponse response = new AutosaveResponse();
        response.setAttemptId(attemptId);
        response.setQuestionId(questionId);
        response.setRevision(saved.getRevision());
        response.setSavedAtUtc(saved.getSavedAt());
        response.setServerNowUtc(now);
        response.setDeadlineAtUtc(attempt.getDeadlineAt());
        return response;
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
