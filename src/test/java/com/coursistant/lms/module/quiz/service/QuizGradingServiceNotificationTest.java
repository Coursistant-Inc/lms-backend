package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.quiz.dto.grading.GradeAnswerRequest;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import com.coursistant.lms.module.quiz.entity.QuizAttemptAnswer;
import com.coursistant.lms.module.quiz.entity.QuizGrade;
import com.coursistant.lms.module.quiz.entity.QuizQuestion;
import com.coursistant.lms.module.quiz.entity.QuizScoreAudit;
import com.coursistant.lms.module.quiz.repository.QuizAttemptAnswerMapper;
import com.coursistant.lms.module.quiz.repository.QuizAttemptMapper;
import com.coursistant.lms.module.quiz.repository.QuizGradeMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.quiz.repository.QuizQuestionMapper;
import com.coursistant.lms.module.quiz.repository.QuizScoreAuditMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizGradingServiceNotificationTest {

    @Mock private QuizMapper quizMapper;
    @Mock private QuizQuestionMapper quizQuestionMapper;
    @Mock private QuizAttemptMapper quizAttemptMapper;
    @Mock private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    @Mock private QuizGradeMapper quizGradeMapper;
    @Mock private QuizScoreAuditMapper quizScoreAuditMapper;
    @Mock private QuizAccessService quizAccessService;
    @Mock private QuizTimeSupport quizTimeSupport;
    @Mock private QuizAuditService quizAuditService;
    @Mock private CourseMapper courseMapper;
    @Mock private NotificationMessageFactory notificationMessageFactory;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private NotificationTimeSupport notificationTimeSupport;

    @InjectMocks
    private QuizGradingService quizGradingService;

    @Test
    void gradeAnswer_nonCountedReleasedAttempt_skipsInstructorReasonAndNotify() {
        stubBaseGradeAnswer(100);
        when(quizGradeMapper.selectByQuizIdAndUserId(9, 50)).thenReturn(releasedGrade(200));
        when(quizTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0, 0));

        GradeAnswerRequest body = new GradeAnswerRequest();
        body.setScore(new BigDecimal("1.0"));
        body.setFeedback("ok");

        quizGradingService.gradeAnswer(1, 9, 100, 7, 20, body);

        verify(quizAccessService, never()).requireInstructor(1, 20);
        verify(notificationPublisher, never()).publishInTransaction(any());
        verify(quizScoreAuditMapper).insert(any(QuizScoreAudit.class));
    }

    @Test
    void gradeAnswer_countedReleased_nonInstructor_throwsNotCourseInstructor() {
        stubAccessAndQuestionAndAttempt(100);
        when(quizGradeMapper.selectByQuizIdAndUserId(9, 50)).thenReturn(releasedGrade(100));
        doThrow(new ApiException(ErrorType.NOT_COURSE_INSTRUCTOR))
                .when(quizAccessService).requireInstructor(1, 20);

        GradeAnswerRequest body = new GradeAnswerRequest();
        body.setScore(new BigDecimal("1.0"));
        body.setReason("fix");

        ApiException ex = assertThrows(ApiException.class,
                () -> quizGradingService.gradeAnswer(1, 9, 100, 7, 20, body));
        assertEquals(ErrorType.NOT_COURSE_INSTRUCTOR, ex.getErrorType());
        verify(notificationPublisher, never()).publishInTransaction(any());
    }

    @Test
    void gradeAnswer_countedReleased_missingReason_throwsBadRequest() {
        stubAccessAndQuestionAndAttempt(100);
        when(quizGradeMapper.selectByQuizIdAndUserId(9, 50)).thenReturn(releasedGrade(100));
        doNothing().when(quizAccessService).requireInstructor(1, 20);

        GradeAnswerRequest body = new GradeAnswerRequest();
        body.setScore(new BigDecimal("1.0"));

        ApiException ex = assertThrows(ApiException.class,
                () -> quizGradingService.gradeAnswer(1, 9, 100, 7, 20, body));
        assertEquals(ErrorType.BAD_REQUEST, ex.getErrorType());
        verify(notificationPublisher, never()).publishInTransaction(any());
    }

    @Test
    void gradeAnswer_countedReleased_visibleChange_dispatchesCorrected() {
        stubBaseGradeAnswer(100);
        when(quizGradeMapper.selectByQuizIdAndUserId(9, 50)).thenReturn(releasedGrade(100));
        doNothing().when(quizAccessService).requireInstructor(1, 20);
        when(quizTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0, 0));
        when(quizScoreAuditMapper.insert(any(QuizScoreAudit.class))).thenAnswer(inv -> {
            QuizScoreAudit audit = inv.getArgument(0);
            audit.setId(55);
            return 1;
        });

        Course course = new Course();
        course.setId(1);
        course.setTenantId(3);
        when(courseMapper.selectById(1)).thenReturn(course);
        Quiz quiz = new Quiz();
        quiz.setId(9);
        quiz.setTitle("Midterm");
        when(quizMapper.selectById(9)).thenReturn(quiz);
        when(notificationMessageFactory.quizGradeCorrected("Midterm")).thenReturn("Quiz grade corrected: Midterm");
        when(notificationPublisher.publishInTransaction(any())).thenReturn(1L);

        GradeAnswerRequest body = new GradeAnswerRequest();
        body.setScore(new BigDecimal("1.0"));
        body.setReason("rubric fix");

        quizGradingService.gradeAnswer(1, 9, 100, 7, 20, body);

        ArgumentCaptor<NotificationDispatchPayload> captor =
                ArgumentCaptor.forClass(NotificationDispatchPayload.class);
        verify(notificationPublisher).publishInTransaction(captor.capture());
        assertEquals(NotificationType.QUIZ_GRADE_CORRECTED, captor.getValue().getNotificationType());
        assertEquals("correct:55", captor.getValue().getEventKey());
    }

    private void stubAccessAndQuestionAndAttempt(int attemptId) {
        doNothing().when(quizAccessService).requireGradingAccess(1, 9, 20);
        when(quizAccessService.requireGradingWritable(1, 20)).thenReturn(new Course());

        QuizQuestion question = new QuizQuestion();
        question.setId(7);
        question.setQuizId(9);
        question.setType(QuizConstants.TYPE_SHORT_ANSWER);
        question.setPoints(new BigDecimal("5.0"));
        when(quizQuestionMapper.selectByQuizIdAndId(9, 7)).thenReturn(question);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(attemptId);
        attempt.setQuizId(9);
        attempt.setUserId(50);
        attempt.setStatus(QuizConstants.ATTEMPT_SUBMITTED);
        attempt.setAutoScore(BigDecimal.ZERO);
        when(quizAttemptMapper.selectByQuizIdAndId(9, attemptId)).thenReturn(attempt);

        QuizAttemptAnswer answer = new QuizAttemptAnswer();
        answer.setAttemptId(attemptId);
        answer.setQuestionId(7);
        answer.setScore(BigDecimal.ZERO);
        answer.setFeedback(null);
        when(quizAttemptAnswerMapper.selectByAttemptIdAndQuestionId(attemptId, 7)).thenReturn(answer);
    }

    private void stubBaseGradeAnswer(int attemptId) {
        stubAccessAndQuestionAndAttempt(attemptId);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(attemptId);
        attempt.setQuizId(9);
        attempt.setUserId(50);
        attempt.setStatus(QuizConstants.ATTEMPT_SUBMITTED);
        attempt.setAutoScore(BigDecimal.ZERO);
        when(quizAttemptMapper.selectById(attemptId)).thenReturn(attempt);
        when(quizAttemptAnswerMapper.selectByAttemptId(attemptId)).thenReturn(Collections.emptyList());
        when(quizAuditService.log(eq(1), eq(9), eq(attemptId), eq(20), eq("ANSWER_GRADED"), any(), isNull()))
                .thenReturn(1);
    }

    private static QuizGrade releasedGrade(Integer countedAttemptId) {
        QuizGrade grade = new QuizGrade();
        grade.setId(3);
        grade.setQuizId(9);
        grade.setUserId(50);
        grade.setStatus(QuizConstants.GRADE_RELEASED);
        grade.setCountedAttemptId(countedAttemptId);
        return grade;
    }
}
