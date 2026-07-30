package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.quiz.dto.attempt.AttemptResponse;
import com.coursistant.lms.module.quiz.dto.attempt.AttemptSummaryResponse;
import com.coursistant.lms.module.quiz.dto.attempt.ReceiptResponse;
import com.coursistant.lms.module.quiz.dto.attempt.SavedAnswerResponse;
import com.coursistant.lms.module.quiz.entity.*;
import com.coursistant.lms.module.quiz.repository.*;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuizAttemptService {

    @Resource
    private QuizMapper quizMapper;
    @Resource
    private QuizAttemptMapper quizAttemptMapper;
    @Resource
    private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    @Resource
    private QuizAccessService quizAccessService;
    @Resource
    private QuizTimeSupport quizTimeSupport;
    @Resource
    private QuizJsonUtil quizJsonUtil;
    @Resource
    private QuizFinalizeService quizFinalizeService;
    @Resource
    private QuizAuditService quizAuditService;

    @Transactional
    public AttemptResponse start(Integer courseId, Integer quizId, Integer userId) {
        quizAccessService.requireNewActivityEnabled();
        Course course = quizAccessService.requireCourse(courseId);
        quizAccessService.requireNotArchived(course);
        quizAccessService.requireCanTakeQuiz(courseId, userId);
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        if (quiz == null) {
            throw new ApiException(ErrorType.QUIZ_NOT_FOUND);
        }
        quizAccessService.assertQuizPublished(quiz);
        quizAccessService.assertQuizWindowOpen(quiz);

        QuizAttempt existing = quizAttemptMapper.selectInProgressByQuizIdAndUserId(quizId, userId);
        if (existing != null) {
            return maybeFinalizeAndBuild(existing, courseId, quiz);
        }

        int submittedCount = quizAttemptMapper.countByQuizIdAndUserId(quizId, userId);
        if (submittedCount >= quiz.getAttemptsAllowed()) {
            throw new ApiException(ErrorType.QUIZ_ATTEMPTS_EXCEEDED);
        }

        var now = quizTimeSupport.nowUtc();
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quizId);
        attempt.setUserId(userId);
        attempt.setAttemptNumber(submittedCount + 1);
        attempt.setStatus(QuizConstants.ATTEMPT_IN_PROGRESS);
        attempt.setStartedAt(now);
        attempt.setDeadlineAt(quizTimeSupport.computeDeadline(now, quiz.getClosesAt(), quiz.getTimeLimitSeconds()));
        attempt.setVersion(1);
        attempt.setCreatedAt(now);
        attempt.setUpdatedAt(now);
        try {
            quizAttemptMapper.insert(attempt);
        } catch (DuplicateKeyException e) {
            QuizAttempt raced = quizAttemptMapper.selectInProgressByQuizIdAndUserId(quizId, userId);
            if (raced != null) {
                return maybeFinalizeAndBuild(raced, courseId, quiz);
            }
            throw e;
        }
        quizAuditService.log(courseId, quizId, attempt.getId(), userId, "ATTEMPT_STARTED", null,
                Map.of("attemptNumber", attempt.getAttemptNumber()));
        return buildResponse(attempt, quiz);
    }

    public AttemptResponse getCurrent(Integer courseId, Integer quizId, Integer userId) {
        quizAccessService.requireCanTakeQuiz(courseId, userId);
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        if (quiz == null) {
            throw new ApiException(ErrorType.QUIZ_NOT_FOUND);
        }
        QuizAttempt attempt = quizAttemptMapper.selectInProgressByQuizIdAndUserId(quizId, userId);
        if (attempt == null) {
            throw new ApiException(ErrorType.QUIZ_ATTEMPT_NOT_FOUND);
        }
        return maybeFinalizeAndBuild(attempt, courseId, quiz);
    }

    public AttemptResponse getAttempt(HttpServletRequest request, Integer courseId, Integer quizId,
                                      Integer attemptId, Integer userId) {
        Quiz quiz = quizAccessService.requireQuizReadable(request, courseId, quizId, userId);
        QuizAttempt attempt = requireAttempt(quizId, attemptId);
        assertAttemptReadable(request, courseId, quizId, attempt, userId);
        return buildResponse(attempt, quiz);
    }

    public List<AttemptSummaryResponse> listAttempts(HttpServletRequest request, Integer courseId, Integer quizId,
                                                     Integer userId, Integer filterUserId, int page, int pageSize) {
        quizAccessService.requireQuizReadable(request, courseId, quizId, userId);
        boolean ownerOnly = !quizAccessService.isStaffViewer(request, courseId, userId)
                && !quizAccessService.isGradingTa(courseId, userId, quizId);
        Integer targetUser = ownerOnly ? userId : filterUserId;
        if (ownerOnly || targetUser == null) {
            targetUser = userId;
        }
        int offset = Math.max(0, (page - 1) * pageSize);
        List<QuizAttempt> attempts = quizAttemptMapper.selectByQuizId(quizId, targetUser, offset, pageSize);
        List<AttemptSummaryResponse> out = new ArrayList<>();
        for (QuizAttempt a : attempts) {
            AttemptSummaryResponse s = new AttemptSummaryResponse();
            s.setId(a.getId());
            s.setAttemptNumber(a.getAttemptNumber());
            s.setStatus(a.getStatus());
            s.setCloseReason(a.getCloseReason());
            s.setStartedAt(a.getStartedAt());
            s.setSubmittedAt(quizTimeSupport.toInstant(a.getSubmittedAt()));
            s.setReceiptId(a.getReceiptId());
            out.add(s);
        }
        return out;
    }

    @Transactional
    public AttemptResponse submit(Integer courseId, Integer quizId, Integer attemptId, Integer userId) {
        QuizAttempt attempt = requireOwnedInProgress(courseId, quizId, attemptId, userId);
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        attempt = quizFinalizeService.finalizeAttempt(attempt.getId(), QuizConstants.CLOSE_MANUAL);
        return buildResponse(attempt, quiz);
    }

    public ReceiptResponse getReceipt(Integer courseId, Integer quizId, Integer attemptId, Integer userId) {
        QuizAttempt attempt = requireAttempt(quizId, attemptId);
        if (!attempt.getUserId().equals(userId)
                && !quizAccessService.isInstructor(courseId, userId)
                && !quizAccessService.isGradingTa(courseId, userId, quizId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED);
        }
        if (attempt.getReceiptId() == null) {
            throw new ApiException(ErrorType.QUIZ_ATTEMPT_NOT_FOUND, "Receipt not issued");
        }
        ReceiptResponse r = new ReceiptResponse();
        r.setAttemptId(attemptId);
        r.setReceiptId(attempt.getReceiptId());
        r.setSubmittedAt(quizTimeSupport.toInstant(attempt.getSubmittedAt()));
        return r;
    }

    private AttemptResponse maybeFinalizeAndBuild(QuizAttempt attempt, Integer courseId, Quiz quiz) {
        if (shouldFinalize(attempt, quiz, courseId)) {
            String reason = resolveCloseReason(attempt, quiz, courseId);
            attempt = quizFinalizeService.finalizeAttempt(attempt.getId(), reason);
        }
        return buildResponse(attempt, quiz);
    }

    public boolean shouldFinalize(QuizAttempt attempt, Quiz quiz, Integer courseId) {
        if (!QuizConstants.ATTEMPT_IN_PROGRESS.equals(attempt.getStatus())) {
            return false;
        }
        var now = quizTimeSupport.nowUtc();
        if (!now.isBefore(attempt.getDeadlineAt())) {
            return true;
        }
        if (!now.isBefore(quiz.getClosesAt())) {
            return true;
        }
        Course course = quizAccessService.requireCourse(courseId);
        if (QuizConstants.COURSE_ARCHIVED.equals(course.getState())) {
            return true;
        }
        return !quizAccessService.isStudentActive(courseId, attempt.getUserId());
    }

    public String resolveCloseReason(QuizAttempt attempt, Quiz quiz, Integer courseId) {
        var now = quizTimeSupport.nowUtc();
        Course course = quizAccessService.requireCourse(courseId);
        if (QuizConstants.COURSE_ARCHIVED.equals(course.getState())) {
            return QuizConstants.CLOSE_COURSE_ARCHIVED;
        }
        if (!quizAccessService.isStudentActive(courseId, attempt.getUserId())) {
            return QuizConstants.CLOSE_MEMBERSHIP_INELIGIBLE;
        }
        if (!now.isBefore(quiz.getClosesAt())) {
            return QuizConstants.CLOSE_QUIZ_CLOSED;
        }
        return QuizConstants.CLOSE_TIME_LIMIT;
    }

    public QuizAttempt requireOwnedInProgress(Integer courseId, Integer quizId, Integer attemptId, Integer userId) {
        QuizAttempt attempt = requireAttempt(quizId, attemptId);
        if (!attempt.getUserId().equals(userId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED);
        }
        if (!QuizConstants.ATTEMPT_IN_PROGRESS.equals(attempt.getStatus())) {
            if (QuizConstants.ATTEMPT_SUBMITTED.equals(attempt.getStatus())) {
                return attempt;
            }
            throw new ApiException(ErrorType.QUIZ_ATTEMPT_NOT_IN_PROGRESS);
        }
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        if (shouldFinalize(attempt, quiz, courseId)) {
            return quizFinalizeService.finalizeAttempt(attemptId, resolveCloseReason(attempt, quiz, courseId));
        }
        return attempt;
    }

    private QuizAttempt requireAttempt(Integer quizId, Integer attemptId) {
        QuizAttempt attempt = quizAttemptMapper.selectByQuizIdAndId(quizId, attemptId);
        if (attempt == null) {
            throw new ApiException(ErrorType.QUIZ_ATTEMPT_NOT_FOUND);
        }
        return attempt;
    }

    private void assertAttemptReadable(HttpServletRequest request, Integer courseId, Integer quizId,
                                       QuizAttempt attempt, Integer userId) {
        if (attempt.getUserId().equals(userId)) {
            return;
        }
        if (quizAccessService.isStaffViewer(request, courseId, userId)) {
            return;
        }
        if (quizAccessService.isGradingTa(courseId, userId, quizId)) {
            return;
        }
        throw new ApiException(ErrorType.ACCESS_DENIED);
    }

    AttemptResponse buildResponse(QuizAttempt attempt, Quiz quiz) {
        AttemptResponse r = new AttemptResponse();
        r.setId(attempt.getId());
        r.setQuizId(attempt.getQuizId());
        r.setUserId(attempt.getUserId());
        r.setAttemptNumber(attempt.getAttemptNumber());
        r.setStatus(attempt.getStatus());
        r.setCloseReason(attempt.getCloseReason());
        r.setReceiptId(attempt.getReceiptId());
        r.setStartedAt(attempt.getStartedAt());
        r.setDeadlineAt(attempt.getDeadlineAt());
        r.setSubmittedAt(quizTimeSupport.toInstant(attempt.getSubmittedAt()));
        r.setServerNowUtc(quizTimeSupport.nowUtc());
        r.setAutoScore(attempt.getAutoScore());
        r.setManualScore(attempt.getManualScore());
        r.setTotalScore(attempt.getTotalScore());
        r.setManualGradingComplete(attempt.getManualGradingComplete());
        List<SavedAnswerResponse> answers = new ArrayList<>();
        for (QuizAttemptAnswer a : quizAttemptAnswerMapper.selectByAttemptId(attempt.getId())) {
            SavedAnswerResponse sa = new SavedAnswerResponse();
            sa.setQuestionId(a.getQuestionId());
            sa.setSelectedOptionIds(quizJsonUtil.parseOptionIds(a.getSelectedOptionIdsJson()));
            sa.setTextAnswer(a.getTextAnswer());
            sa.setRevision(a.getRevision());
            sa.setSavedAt(a.getSavedAt());
            answers.add(sa);
        }
        r.setAnswers(answers);
        return r;
    }
}
