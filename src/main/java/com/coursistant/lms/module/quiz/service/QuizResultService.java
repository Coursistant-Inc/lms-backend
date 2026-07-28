package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.dto.result.MyResultResponse;
import com.coursistant.lms.module.quiz.dto.result.QuestionResultItem;
import com.coursistant.lms.module.quiz.entity.*;
import com.coursistant.lms.module.quiz.repository.*;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizResultService {

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
    private QuizAccessService quizAccessService;
    @Resource
    private QuizJsonUtil quizJsonUtil;
    @Resource
    private QuizTimeSupport quizTimeSupport;

    public MyResultResponse myResult(Integer courseId, Integer quizId, Integer userId) {
        quizAccessService.requireCanTakeQuiz(courseId, userId);
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        if (quiz == null || !QuizConstants.STATE_PUBLISHED.equals(quiz.getState())) {
            throw new ApiException(ErrorType.QUIZ_NOT_FOUND);
        }
        QuizGrade grade = quizGradeMapper.selectByQuizIdAndUserId(quizId, userId);
        if (grade == null) {
            throw new ApiException(ErrorType.NOT_FOUND, "No grade yet");
        }
        QuizAttempt attempt = quizAttemptMapper.selectById(grade.getCountedAttemptId());
        return buildResult(quiz, grade, attempt, userId);
    }

    public MyResultResponse attemptResult(Integer courseId, Integer quizId, Integer attemptId, Integer userId) {
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        if (quiz == null) {
            throw new ApiException(ErrorType.QUIZ_NOT_FOUND);
        }
        QuizAttempt attempt = quizAttemptMapper.selectByQuizIdAndId(quizId, attemptId);
        if (attempt == null) {
            throw new ApiException(ErrorType.QUIZ_ATTEMPT_NOT_FOUND);
        }
        if (!attempt.getUserId().equals(userId)
                && !quizAccessService.isInstructor(courseId, userId)
                && !quizAccessService.isGradingTa(courseId, userId, quizId)) {
            throw new ApiException(ErrorType.ACCESS_DENIED);
        }
        QuizGrade grade = quizGradeMapper.selectByQuizIdAndUserId(quizId, attempt.getUserId());
        return buildResult(quiz, grade, attempt, attempt.getUserId());
    }

    public List<MyResultResponse> myAttemptsSummary(Integer courseId, Integer quizId, Integer userId) {
        quizAccessService.requireCanTakeQuiz(courseId, userId);
        List<QuizAttempt> attempts = quizAttemptMapper.selectByQuizIdAndUserId(quizId, userId);
        List<MyResultResponse> out = new ArrayList<>();
        Quiz quiz = quizMapper.selectByCourseIdAndId(courseId, quizId);
        QuizGrade grade = quizGradeMapper.selectByQuizIdAndUserId(quizId, userId);
        for (QuizAttempt a : attempts) {
            MyResultResponse r = new MyResultResponse();
            r.setQuizId(quizId);
            r.setCountedAttemptId(a.getId());
            r.setCloseReason(a.getCloseReason());
            r.setReceiptId(a.getReceiptId());
            if (grade != null && grade.getCountedAttemptId().equals(a.getId())) {
                r.setGradeStatus(grade.getStatus());
            }
            out.add(r);
        }
        return out;
    }

    MyResultResponse buildResult(Quiz quiz, QuizGrade grade, QuizAttempt attempt, Integer studentUserId) {
        MyResultResponse r = new MyResultResponse();
        r.setQuizId(quiz.getId());
        r.setCountedAttemptId(attempt.getId());
        r.setCloseReason(attempt.getCloseReason());
        r.setReceiptId(attempt.getReceiptId());
        if (grade != null) {
            r.setGradeStatus(grade.getStatus());
        }
        boolean released = grade != null && QuizConstants.GRADE_RELEASED.equals(grade.getStatus());
        boolean instant = QuizConstants.VISIBILITY_INSTANT_AUTO.equals(quiz.getResultVisibility());
        boolean manualPending = !Boolean.TRUE.equals(attempt.getManualGradingComplete());
        boolean quizClosed = !quizTimeSupport.nowUtc().isBefore(quiz.getClosesAt());

        applyVisibility(r, instant, released, manualPending, attempt);

        List<QuestionResultItem> questions = new ArrayList<>();
        for (QuizQuestion q : quizQuestionMapper.selectByQuizId(quiz.getId())) {
            QuestionResultItem item = new QuestionResultItem();
            item.setQuestionId(q.getId());
            item.setType(q.getType());
            item.setPoints(q.getPoints());
            QuizAttemptAnswer ans = quizAttemptAnswerMapper.selectByAttemptIdAndQuestionId(attempt.getId(), q.getId());
            if (ans != null) {
                item.setSelectedOptionIds(quizJsonUtil.parseOptionIds(ans.getSelectedOptionIdsJson()));
                item.setTextAnswer(ans.getTextAnswer());
                if (shouldShowQuestionScore(instant, released, manualPending)) {
                    item.setScore(ans.getScore());
                }
            }
            if (quizClosed && released && !QuizConstants.TYPE_SHORT_ANSWER.equals(q.getType())) {
                item.setCorrectOptionIds(quizQuestionOptionMapper.selectInstructorByQuestionId(q.getId()).stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .map(QuizQuestionOption::getId)
                        .collect(Collectors.toList()));
                r.setShowCorrectAnswers(true);
            }
            questions.add(item);
        }
        r.setQuestions(questions);
        return r;
    }

    static void applyVisibility(MyResultResponse r, boolean instant, boolean released,
                                boolean manualPending, QuizAttempt attempt) {
        if (instant) {
            r.setAutoScore(attempt.getAutoScore());
            r.setManualGradingPending(manualPending);
            if (released && !manualPending) {
                r.setManualScore(attempt.getManualScore());
                r.setTotalScore(attempt.getTotalScore());
            }
            return;
        }
        if (released) {
            r.setAutoScore(attempt.getAutoScore());
            r.setManualGradingPending(manualPending);
            if (!manualPending) {
                r.setManualScore(attempt.getManualScore());
                r.setTotalScore(attempt.getTotalScore());
            }
        }
    }

    static boolean shouldShowQuestionScore(boolean instant, boolean released, boolean manualPending) {
        if (instant) {
            return true;
        }
        return released;
    }
}
