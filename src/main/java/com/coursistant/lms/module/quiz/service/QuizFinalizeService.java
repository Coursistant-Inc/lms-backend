package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.dto.attempt.AttemptResponse;
import com.coursistant.lms.module.quiz.entity.*;
import com.coursistant.lms.module.quiz.repository.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuizFinalizeService {

    @Resource
    private QuizMapper quizMapper;
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
    public QuizAttempt finalizeAttempt(Integer attemptId, String closeReason) {
        QuizAttempt attempt = quizAttemptMapper.selectById(attemptId);
        if (attempt == null) {
            return null;
        }
        if (QuizConstants.ATTEMPT_SUBMITTED.equals(attempt.getStatus())) {
            return attempt;
        }
        if (!QuizConstants.ATTEMPT_IN_PROGRESS.equals(attempt.getStatus())) {
            return attempt;
        }
        if (quizAttemptMapper.casToFinalizing(attemptId) == 0) {
            return quizAttemptMapper.selectById(attemptId);
        }

        Quiz quiz = quizMapper.selectById(attempt.getQuizId());
        List<QuizQuestion> questions = quizQuestionMapper.selectByQuizId(quiz.getId());
        BigDecimal autoTotal = BigDecimal.ZERO;
        BigDecimal manualTotal = BigDecimal.ZERO;
        boolean pendingManual = false;

        for (QuizQuestion question : questions) {
            QuizAttemptAnswer answer = quizAttemptAnswerMapper.selectByAttemptIdAndQuestionId(
                    attemptId, question.getId());
            if (answer == null) {
                if (QuizConstants.TYPE_SHORT_ANSWER.equals(question.getType())) {
                    pendingManual = true;
                }
                continue;
            }
            if (QuizConstants.TYPE_SHORT_ANSWER.equals(question.getType())) {
                answer.setPendingManual(true);
                answer.setScore(null);
                answer.setUpdatedAt(quizTimeSupport.nowUtc());
                quizAttemptAnswerMapper.updateScore(answer);
                pendingManual = true;
            } else {
                List<QuizQuestionOption> options = quizQuestionOptionMapper.selectInstructorByQuestionId(question.getId());
                List<Integer> selected = quizJsonUtil.parseOptionIds(answer.getSelectedOptionIdsJson());
                BigDecimal score = quizScoringService.scoreObjective(question, selected, options);
                answer.setScore(score);
                answer.setPendingManual(false);
                answer.setUpdatedAt(quizTimeSupport.nowUtc());
                quizAttemptAnswerMapper.updateScore(answer);
                autoTotal = autoTotal.add(score == null ? BigDecimal.ZERO : score);
            }
        }

        var now = quizTimeSupport.nowUtc();
        String receiptId = attempt.getReceiptId() != null ? attempt.getReceiptId() : UUID.randomUUID().toString();
        attempt.setStatus(QuizConstants.ATTEMPT_SUBMITTED);
        attempt.setCloseReason(closeReason);
        attempt.setReceiptId(receiptId);
        attempt.setSubmittedAt(now);
        attempt.setAutoScore(autoTotal);
        attempt.setManualScore(manualTotal);
        attempt.setTotalScore(autoTotal.add(manualTotal));
        attempt.setManualGradingComplete(!pendingManual);
        attempt.setUpdatedAt(now);
        quizAttemptMapper.updateSubmitted(attempt);

        QuizGrade grade = new QuizGrade();
        grade.setQuizId(quiz.getId());
        grade.setUserId(attempt.getUserId());
        grade.setCountedAttemptId(attemptId);
        grade.setStatus(QuizConstants.GRADE_ENTERED);
        grade.setCreatedAt(now);
        grade.setUpdatedAt(now);
        quizGradeMapper.upsertOnSubmit(grade);

        String action = QuizConstants.CLOSE_MANUAL.equals(closeReason)
                ? "ATTEMPT_SUBMITTED" : "ATTEMPT_AUTO_SUBMITTED";
        quizAuditService.log(quiz.getCourseId(), quiz.getId(), attemptId, attempt.getUserId(),
                action, closeReason, Map.of("receiptId", receiptId));
        return quizAttemptMapper.selectById(attemptId);
    }
}
