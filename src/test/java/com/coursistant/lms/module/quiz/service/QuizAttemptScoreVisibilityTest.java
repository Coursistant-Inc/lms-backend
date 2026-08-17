package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.quiz.dto.attempt.AttemptResponse;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import com.coursistant.lms.module.quiz.entity.QuizGrade;
import com.coursistant.lms.module.quiz.repository.QuizAttemptAnswerMapper;
import com.coursistant.lms.module.quiz.repository.QuizAttemptMapper;
import com.coursistant.lms.module.quiz.repository.QuizGradeMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizAttemptScoreVisibilityTest {

    private static final int COURSE_ID = 1;
    private static final int QUIZ_ID = 9;
    private static final int ATTEMPT_ID = 50;
    private static final int OWNER_ID = 21;
    private static final int STAFF_ID = 7;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 12, 0, 0);

    @Mock private QuizMapper quizMapper;
    @Mock private QuizAttemptMapper quizAttemptMapper;
    @Mock private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    @Mock private QuizAccessService quizAccessService;
    @Mock private QuizTimeSupport quizTimeSupport;
    @Mock private QuizJsonUtil quizJsonUtil;
    @Mock private QuizFinalizeService quizFinalizeService;
    @Mock private QuizAuditService quizAuditService;
    @Mock private QuizGradeMapper quizGradeMapper;

    @InjectMocks
    private QuizAttemptService quizAttemptService;

    @BeforeEach
    void stubClockAndAnswers() {
        lenient().when(quizTimeSupport.nowUtc()).thenReturn(NOW);
        lenient().when(quizTimeSupport.toInstant(any())).thenReturn(Instant.parse("2026-08-17T12:00:00Z"));
        lenient().when(quizAttemptAnswerMapper.selectByAttemptId(ATTEMPT_ID)).thenReturn(Collections.emptyList());
    }

    @Test
    void submit_afterReleaseEntered_hidesAllScoresForOwner() {
        Quiz quiz = afterReleaseQuiz();
        QuizAttempt inProgress = inProgressAttempt();
        QuizAttempt submitted = scoredSubmittedAttempt(false);
        stubSubmitPath(quiz, inProgress, submitted);
        when(quizGradeMapper.selectByQuizIdAndUserId(QUIZ_ID, OWNER_ID)).thenReturn(enteredGrade());

        AttemptResponse r = quizAttemptService.submit(COURSE_ID, QUIZ_ID, ATTEMPT_ID, OWNER_ID);

        assertScoresHidden(r);
        assertNull(r.getManualGradingComplete());
    }

    @Test
    void getCurrent_autoFinalize_afterReleaseEntered_hidesAllScoresForOwner() {
        Quiz quiz = afterReleaseQuiz();
        quiz.setClosesAt(NOW.plusHours(2));
        QuizAttempt inProgress = inProgressAttempt();
        inProgress.setDeadlineAt(NOW.minusMinutes(1));
        QuizAttempt submitted = scoredSubmittedAttempt(false);
        Course course = new Course();
        course.setState("Active");
        when(quizMapper.selectByCourseIdAndId(COURSE_ID, QUIZ_ID)).thenReturn(quiz);
        when(quizAttemptMapper.selectInProgressByQuizIdAndUserId(QUIZ_ID, OWNER_ID)).thenReturn(inProgress);
        when(quizAccessService.requireCourse(COURSE_ID)).thenReturn(course);
        when(quizAccessService.isStudentActive(COURSE_ID, OWNER_ID)).thenReturn(true);
        when(quizFinalizeService.finalizeAttempt(ATTEMPT_ID, QuizConstants.CLOSE_TIME_LIMIT)).thenReturn(submitted);
        when(quizGradeMapper.selectByQuizIdAndUserId(QUIZ_ID, OWNER_ID)).thenReturn(enteredGrade());

        AttemptResponse r = quizAttemptService.getCurrent(COURSE_ID, QUIZ_ID, OWNER_ID);

        assertScoresHidden(r);
        assertNull(r.getManualGradingComplete());
    }

    @Test
    void getAttempt_owner_afterReleaseEntered_hidesAllScores() {
        AttemptResponse r = getAttemptAsOwner(afterReleaseQuiz(), scoredSubmittedAttempt(true), enteredGrade());

        assertScoresHidden(r);
        assertNull(r.getManualGradingComplete());
    }

    @Test
    void getAttempt_owner_afterReleaseReleasedPending_showsAutoOnly() {
        AttemptResponse r = getAttemptAsOwner(
                afterReleaseQuiz(), scoredSubmittedAttempt(false), releasedGrade());

        assertEquals(new BigDecimal("7"), r.getAutoScore());
        assertNull(r.getManualScore());
        assertNull(r.getTotalScore());
        assertFalse(r.getManualGradingComplete());
    }

    @Test
    void getAttempt_owner_afterReleaseReleasedComplete_showsAllScores() {
        AttemptResponse r = getAttemptAsOwner(
                afterReleaseQuiz(), scoredSubmittedAttempt(true), releasedGrade());

        assertEquals(new BigDecimal("7"), r.getAutoScore());
        assertEquals(new BigDecimal("3"), r.getManualScore());
        assertEquals(new BigDecimal("10"), r.getTotalScore());
        assertEquals(Boolean.TRUE, r.getManualGradingComplete());
    }

    @Test
    void getAttempt_owner_instantAutoEntered_showsAutoAndGradingStatus() {
        Quiz quiz = afterReleaseQuiz();
        quiz.setResultVisibility(QuizConstants.VISIBILITY_INSTANT_AUTO);
        AttemptResponse r = getAttemptAsOwner(quiz, scoredSubmittedAttempt(false), enteredGrade());

        assertEquals(new BigDecimal("7"), r.getAutoScore());
        assertNull(r.getManualScore());
        assertNull(r.getTotalScore());
        assertFalse(r.getManualGradingComplete());
    }

    @Test
    void getAttempt_nonOwnerStaff_seesFullScoresWhenUnreleased() {
        Quiz quiz = afterReleaseQuiz();
        QuizAttempt attempt = scoredSubmittedAttempt(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(quizAccessService.requireQuizReadable(request, COURSE_ID, QUIZ_ID, STAFF_ID)).thenReturn(quiz);
        when(quizAttemptMapper.selectByQuizIdAndId(QUIZ_ID, ATTEMPT_ID)).thenReturn(attempt);
        when(quizAccessService.isStaffViewer(request, COURSE_ID, STAFF_ID)).thenReturn(true);

        AttemptResponse r = quizAttemptService.getAttempt(request, COURSE_ID, QUIZ_ID, ATTEMPT_ID, STAFF_ID);

        assertEquals(new BigDecimal("7"), r.getAutoScore());
        assertEquals(new BigDecimal("3"), r.getManualScore());
        assertEquals(new BigDecimal("10"), r.getTotalScore());
        assertEquals(Boolean.TRUE, r.getManualGradingComplete());
    }

    @Test
    void getAttempt_ownerPromotedToStaff_stillHidesUnreleasedScores() {
        Quiz quiz = afterReleaseQuiz();
        QuizAttempt attempt = scoredSubmittedAttempt(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(quizAccessService.requireQuizReadable(request, COURSE_ID, QUIZ_ID, OWNER_ID)).thenReturn(quiz);
        when(quizAttemptMapper.selectByQuizIdAndId(QUIZ_ID, ATTEMPT_ID)).thenReturn(attempt);
        lenient().when(quizAccessService.isStaffViewer(request, COURSE_ID, OWNER_ID)).thenReturn(true);
        lenient().when(quizAccessService.isGradingTa(COURSE_ID, OWNER_ID, QUIZ_ID)).thenReturn(true);
        when(quizGradeMapper.selectByQuizIdAndUserId(QUIZ_ID, OWNER_ID)).thenReturn(enteredGrade());

        AttemptResponse r = quizAttemptService.getAttempt(request, COURSE_ID, QUIZ_ID, ATTEMPT_ID, OWNER_ID);

        assertScoresHidden(r);
        assertNull(r.getManualGradingComplete());
    }

    private AttemptResponse getAttemptAsOwner(Quiz quiz, QuizAttempt attempt, QuizGrade grade) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(quizAccessService.requireQuizReadable(request, COURSE_ID, QUIZ_ID, OWNER_ID)).thenReturn(quiz);
        when(quizAttemptMapper.selectByQuizIdAndId(QUIZ_ID, ATTEMPT_ID)).thenReturn(attempt);
        when(quizGradeMapper.selectByQuizIdAndUserId(QUIZ_ID, OWNER_ID)).thenReturn(grade);
        return quizAttemptService.getAttempt(request, COURSE_ID, QUIZ_ID, ATTEMPT_ID, OWNER_ID);
    }

    private void stubSubmitPath(Quiz quiz, QuizAttempt inProgress, QuizAttempt submitted) {
        quiz.setClosesAt(NOW.plusHours(2));
        Course course = new Course();
        course.setState("Active");
        when(quizAttemptMapper.selectByQuizIdAndId(QUIZ_ID, ATTEMPT_ID)).thenReturn(inProgress);
        when(quizMapper.selectByCourseIdAndId(COURSE_ID, QUIZ_ID)).thenReturn(quiz);
        when(quizAccessService.requireCourse(COURSE_ID)).thenReturn(course);
        when(quizAccessService.isStudentActive(COURSE_ID, OWNER_ID)).thenReturn(true);
        when(quizFinalizeService.finalizeAttempt(ATTEMPT_ID, QuizConstants.CLOSE_MANUAL)).thenReturn(submitted);
    }

    private static void assertScoresHidden(AttemptResponse r) {
        assertNull(r.getAutoScore());
        assertNull(r.getManualScore());
        assertNull(r.getTotalScore());
    }

    private static Quiz afterReleaseQuiz() {
        Quiz quiz = new Quiz();
        quiz.setId(QUIZ_ID);
        quiz.setCourseId(COURSE_ID);
        quiz.setResultVisibility(QuizConstants.VISIBILITY_AFTER_RELEASE);
        quiz.setClosesAt(NOW.plusHours(2));
        return quiz;
    }

    private static QuizAttempt inProgressAttempt() {
        QuizAttempt a = new QuizAttempt();
        a.setId(ATTEMPT_ID);
        a.setQuizId(QUIZ_ID);
        a.setUserId(OWNER_ID);
        a.setAttemptNumber(1);
        a.setStatus(QuizConstants.ATTEMPT_IN_PROGRESS);
        a.setStartedAt(NOW.minusMinutes(10));
        a.setDeadlineAt(NOW.plusMinutes(20));
        return a;
    }

    private static QuizAttempt scoredSubmittedAttempt(boolean gradingComplete) {
        QuizAttempt a = new QuizAttempt();
        a.setId(ATTEMPT_ID);
        a.setQuizId(QUIZ_ID);
        a.setUserId(OWNER_ID);
        a.setAttemptNumber(1);
        a.setStatus(QuizConstants.ATTEMPT_SUBMITTED);
        a.setCloseReason(QuizConstants.CLOSE_MANUAL);
        a.setStartedAt(NOW.minusMinutes(10));
        a.setDeadlineAt(NOW.plusMinutes(20));
        a.setSubmittedAt(NOW);
        a.setAutoScore(new BigDecimal("7"));
        a.setManualScore(new BigDecimal("3"));
        a.setTotalScore(new BigDecimal("10"));
        a.setManualGradingComplete(gradingComplete);
        return a;
    }

    private static QuizGrade enteredGrade() {
        QuizGrade g = new QuizGrade();
        g.setQuizId(QUIZ_ID);
        g.setUserId(OWNER_ID);
        g.setCountedAttemptId(ATTEMPT_ID);
        g.setStatus(QuizConstants.GRADE_ENTERED);
        return g;
    }

    private static QuizGrade releasedGrade() {
        QuizGrade g = enteredGrade();
        g.setStatus(QuizConstants.GRADE_RELEASED);
        return g;
    }
}
