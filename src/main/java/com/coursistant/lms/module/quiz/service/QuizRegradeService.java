package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.dto.authoring.PatchAnswerKeyRequest;
import com.coursistant.lms.module.quiz.dto.authoring.QuestionResponse;
import com.coursistant.lms.module.quiz.entity.*;
import com.coursistant.lms.module.quiz.repository.*;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizRegradeService {

    @Resource
    private QuizQuestionMapper quizQuestionMapper;
    @Resource
    private QuizQuestionOptionMapper quizQuestionOptionMapper;
    @Resource
    private QuizAttemptMapper quizAttemptMapper;
    @Resource
    private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    @Resource
    private QuizGradeMapper quizGradeMapper;
    @Resource
    private QuizScoringService quizScoringService;
    @Resource
    private QuizJsonUtil quizJsonUtil;
    @Resource
    private QuizTimeSupport quizTimeSupport;
    @Resource
    private QuizAuditService quizAuditService;

    @Transactional
    public QuestionResponse regradeAnswerKey(Integer courseId, Integer quizId, Integer questionId,
                                             Integer userId, PatchAnswerKeyRequest body) {
        if (body == null || body.getOptions() == null || body.getReason() == null || body.getReason().isBlank()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Answer key patch requires options and reason");
        }
        QuizQuestion question = quizQuestionMapper.selectByIdForUpdate(questionId);
        if (question == null || !question.getQuizId().equals(quizId)) {
            throw new ApiException(ErrorType.QUIZ_QUESTION_NOT_FOUND);
        }
        if (body.getExpectedVersion() != null && !body.getExpectedVersion().equals(question.getVersion())) {
            throw new ApiException(ErrorType.QUIZ_VERSION_CONFLICT);
        }
        for (var opt : body.getOptions()) {
            QuizQuestionOption existing = quizQuestionOptionMapper.selectById(opt.getOptionId());
            if (existing == null || !existing.getQuestionId().equals(questionId)) {
                throw new ApiException(ErrorType.BAD_REQUEST, "Invalid option id");
            }
            quizQuestionOptionMapper.updateIsCorrect(opt.getOptionId(), Boolean.TRUE.equals(opt.getIsCorrect()));
        }
        question.setVersion(question.getVersion() + 1);
        question.setUpdatedAt(quizTimeSupport.nowUtc());
        quizQuestionMapper.updateById(question);

        List<QuizQuestionOption> options = quizQuestionOptionMapper.selectInstructorByQuestionId(questionId);
        for (QuizAttemptAnswer answer : quizAttemptAnswerMapper.selectByQuestionId(questionId)) {
            QuizAttempt attempt = quizAttemptMapper.selectById(answer.getAttemptId());
            if (attempt == null || !QuizConstants.ATTEMPT_SUBMITTED.equals(attempt.getStatus())) {
                continue;
            }
            if (QuizConstants.TYPE_SHORT_ANSWER.equals(question.getType())) {
                continue;
            }
            List<Integer> selected = quizJsonUtil.parseOptionIds(answer.getSelectedOptionIdsJson());
            BigDecimal score = quizScoringService.scoreObjective(question, selected, options);
            answer.setScore(score);
            answer.setPendingManual(false);
            answer.setUpdatedAt(quizTimeSupport.nowUtc());
            quizAttemptAnswerMapper.updateScore(answer);
            recalcAttemptTotals(attempt.getId());
            QuizGrade grade = quizGradeMapper.selectByQuizIdAndUserId(quizId, attempt.getUserId());
            if (grade != null) {
                quizGradeMapper.incrementVersion(grade.getId());
            }
        }

        quizAuditService.log(courseId, quizId, null, userId, "ANSWER_KEY_UPDATED", body.getReason(),
                Map.of("questionId", questionId));
        quizAuditService.log(courseId, quizId, null, userId, "SCORES_RECALCULATED", body.getReason(), null);

        return buildResponse(questionId);
    }

    private void recalcAttemptTotals(Integer attemptId) {
        QuizAttempt attempt = quizAttemptMapper.selectById(attemptId);
        List<QuizAttemptAnswer> answers = quizAttemptAnswerMapper.selectByAttemptId(attemptId);
        BigDecimal auto = BigDecimal.ZERO;
        BigDecimal manual = BigDecimal.ZERO;
        boolean pending = false;
        for (QuizAttemptAnswer a : answers) {
            if (Boolean.TRUE.equals(a.getPendingManual())) {
                pending = true;
            } else if (a.getScore() != null) {
                auto = auto.add(a.getScore());
            }
        }
        attempt.setAutoScore(auto);
        attempt.setManualScore(manual);
        attempt.setTotalScore(auto.add(manual));
        attempt.setManualGradingComplete(!pending);
        attempt.setUpdatedAt(quizTimeSupport.nowUtc());
        quizAttemptMapper.updateSubmitted(attempt);
    }

    private QuestionResponse buildResponse(Integer questionId) {
        QuizQuestion q = quizQuestionMapper.selectById(questionId);
        QuestionResponse r = new QuestionResponse();
        r.setId(q.getId());
        r.setQuizId(q.getQuizId());
        r.setType(q.getType());
        r.setStem(q.getStem());
        r.setPoints(q.getPoints());
        r.setPosition(q.getPosition());
        r.setVersion(q.getVersion());
        return r;
    }
}
