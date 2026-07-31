package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.service.NotificationCommitHook;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.quiz.dto.authoring.OptionKeyInput;
import com.coursistant.lms.module.quiz.dto.authoring.PatchAnswerKeyRequest;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import com.coursistant.lms.module.quiz.entity.QuizAttemptAnswer;
import com.coursistant.lms.module.quiz.entity.QuizGrade;
import com.coursistant.lms.module.quiz.entity.QuizQuestion;
import com.coursistant.lms.module.quiz.entity.QuizQuestionOption;
import com.coursistant.lms.module.quiz.repository.QuizAttemptAnswerMapper;
import com.coursistant.lms.module.quiz.repository.QuizAttemptMapper;
import com.coursistant.lms.module.quiz.repository.QuizGradeMapper;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.quiz.repository.QuizQuestionMapper;
import com.coursistant.lms.module.quiz.repository.QuizQuestionOptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizRegradeServiceNotificationTest {

    @Mock private QuizQuestionMapper quizQuestionMapper;
    @Mock private QuizQuestionOptionMapper quizQuestionOptionMapper;
    @Mock private QuizAttemptMapper quizAttemptMapper;
    @Mock private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    @Mock private QuizGradeMapper quizGradeMapper;
    @Mock private QuizScoringService quizScoringService;
    @Mock private QuizJsonUtil quizJsonUtil;
    @Mock private QuizTimeSupport quizTimeSupport;
    @Mock private QuizAuditService quizAuditService;
    @Mock private QuizMapper quizMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private NotificationRecipientResolver notificationRecipientResolver;
    @Mock private NotificationMessageFactory notificationMessageFactory;
    @Mock private NotificationCommitHook notificationCommitHook;
    @Mock private NotificationTimeSupport notificationTimeSupport;

    @InjectMocks
    private QuizRegradeService quizRegradeService;

    @Test
    void regrade_nonCountedReleasedAttempt_doesNotNotify() {
        QuizQuestion question = new QuizQuestion();
        question.setId(7);
        question.setQuizId(9);
        question.setType(QuizConstants.TYPE_SINGLE_CHOICE);
        question.setVersion(1);
        question.setPoints(new BigDecimal("1.0"));
        when(quizQuestionMapper.selectByIdForUpdate(7)).thenReturn(question);
        when(quizQuestionMapper.selectById(7)).thenReturn(question);
        when(quizTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 7, 1, 0, 0));

        OptionKeyInput opt = new OptionKeyInput();
        opt.setOptionId(1);
        opt.setIsCorrect(true);
        PatchAnswerKeyRequest body = new PatchAnswerKeyRequest();
        body.setReason("fix key");
        body.setOptions(List.of(opt));

        QuizQuestionOption existingOpt = new QuizQuestionOption();
        existingOpt.setId(1);
        existingOpt.setQuestionId(7);
        when(quizQuestionOptionMapper.selectById(1)).thenReturn(existingOpt);
        when(quizQuestionOptionMapper.selectInstructorByQuestionId(7)).thenReturn(List.of(existingOpt));

        QuizAttemptAnswer answer = new QuizAttemptAnswer();
        answer.setAttemptId(100);
        answer.setQuestionId(7);
        answer.setScore(BigDecimal.ZERO);
        answer.setSelectedOptionIdsJson("[1]");
        when(quizAttemptAnswerMapper.selectByQuestionId(7)).thenReturn(List.of(answer));
        when(quizAttemptAnswerMapper.selectByAttemptId(100)).thenReturn(List.of(answer));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(100);
        attempt.setUserId(50);
        attempt.setStatus(QuizConstants.ATTEMPT_SUBMITTED);
        when(quizAttemptMapper.selectById(100)).thenReturn(attempt);

        when(quizJsonUtil.parseOptionIds("[1]")).thenReturn(List.of(1));
        when(quizScoringService.scoreObjective(any(), anyList(), anyList())).thenReturn(new BigDecimal("1.0"));

        QuizGrade grade = new QuizGrade();
        grade.setId(3);
        grade.setStatus(QuizConstants.GRADE_RELEASED);
        grade.setCountedAttemptId(999);
        when(quizGradeMapper.selectByQuizIdAndUserId(9, 50)).thenReturn(grade);

        when(quizAuditService.log(eq(1), eq(9), isNull(), eq(20), anyString(), anyString(), anyMap()))
                .thenReturn(1);
        when(quizAuditService.log(eq(1), eq(9), isNull(), eq(20), anyString(), anyString(), isNull()))
                .thenReturn(2);

        quizRegradeService.regradeAnswerKey(1, 9, 7, 20, body);

        verify(notificationCommitHook, never()).afterCommitDispatch(any());
        verify(notificationRecipientResolver, never()).filterCandidateRecipients(any(), anyList());
    }
}
