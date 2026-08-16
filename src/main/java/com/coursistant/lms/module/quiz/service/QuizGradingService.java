package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.service.NotificationCommitHook;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.quiz.dto.grading.GradeAnswerRequest;
import com.coursistant.lms.module.quiz.dto.grading.GradingSummaryResponse;
import com.coursistant.lms.module.quiz.dto.grading.ReleaseGradesRequest;
import com.coursistant.lms.module.quiz.dto.grading.ShortAnswerGradingItem;
import com.coursistant.lms.module.quiz.entity.*;
import com.coursistant.lms.module.quiz.repository.*;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class QuizGradingService {

    @Resource
    private QuizMapper quizMapper;
    @Resource
    private QuizQuestionMapper quizQuestionMapper;
    @Resource
    private QuizAttemptMapper quizAttemptMapper;
    @Resource
    private QuizAttemptAnswerMapper quizAttemptAnswerMapper;
    @Resource
    private QuizGradeMapper quizGradeMapper;
    @Resource
    private QuizScoreAuditMapper quizScoreAuditMapper;
    @Resource
    private QuizAccessService quizAccessService;
    @Resource
    private QuizTimeSupport quizTimeSupport;
    @Resource
    private QuizAuditService quizAuditService;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private NotificationRecipientResolver notificationRecipientResolver;
    @Resource
    private NotificationMessageFactory notificationMessageFactory;
    @Resource
    private NotificationCommitHook notificationCommitHook;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    public GradingSummaryResponse summary(Integer courseId, Integer quizId, Integer userId) {
        quizAccessService.requireGradingAccess(courseId, quizId, userId);
        quizAccessService.requireGradingWritable(courseId, userId);
        GradingSummaryResponse r = new GradingSummaryResponse();
        r.setPendingShortAnswerCount(quizAttemptAnswerMapper.countPendingManualByQuizId(quizId));
        r.setSubmittedAttemptCount(quizAttemptMapper.countSubmittedByQuizId(quizId));
        r.setReleasedUserCount(quizGradeMapper.countReleasedByQuizId(quizId));
        r.setManualIncompleteAttemptCount(countManualIncomplete(quizId));
        return r;
    }

    public List<ShortAnswerGradingItem> listShortAnswers(Integer courseId, Integer quizId,
                                                         Integer questionId, Integer userId) {
        quizAccessService.requireGradingAccess(courseId, quizId, userId);
        QuizQuestion q = quizQuestionMapper.selectByQuizIdAndId(quizId, questionId);
        if (q == null) {
            throw new ApiException(ErrorType.QUIZ_QUESTION_NOT_FOUND);
        }
        List<ShortAnswerGradingItem> out = new ArrayList<>();
        for (QuizAttemptAnswer a : quizAttemptAnswerMapper.selectShortAnswersByQuestionId(questionId)) {
            QuizAttempt att = quizAttemptMapper.selectById(a.getAttemptId());
            ShortAnswerGradingItem item = new ShortAnswerGradingItem();
            item.setAttemptId(a.getAttemptId());
            item.setUserId(att.getUserId());
            item.setQuestionId(questionId);
            item.setTextAnswer(a.getTextAnswer());
            item.setScore(a.getScore());
            item.setPendingManual(a.getPendingManual());
            item.setFeedback(a.getFeedback());
            out.add(item);
        }
        return out;
    }

    @Transactional
    public void gradeAnswer(Integer courseId, Integer quizId, Integer attemptId, Integer questionId,
                            Integer userId, GradeAnswerRequest body) {
        quizAccessService.requireGradingAccess(courseId, quizId, userId);
        quizAccessService.requireGradingWritable(courseId, userId);
        QuizQuestion question = quizQuestionMapper.selectByQuizIdAndId(quizId, questionId);
        if (question == null || !QuizConstants.TYPE_SHORT_ANSWER.equals(question.getType())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Only short answer questions can be manually graded");
        }
        QuizAttempt attempt = quizAttemptMapper.selectByQuizIdAndId(quizId, attemptId);
        if (attempt == null || !QuizConstants.ATTEMPT_SUBMITTED.equals(attempt.getStatus())) {
            throw new ApiException(ErrorType.QUIZ_ATTEMPT_NOT_FOUND);
        }
        QuizAttemptAnswer answer = quizAttemptAnswerMapper.selectByAttemptIdAndQuestionId(attemptId, questionId);
        if (answer == null) {
            throw new ApiException(ErrorType.QUIZ_ANSWER_INVALID);
        }
        QuizGrade grade = quizGradeMapper.selectByQuizIdAndUserId(quizId, attempt.getUserId());
        boolean releasedCountedAttempt =
                grade != null
                        && QuizConstants.GRADE_RELEASED.equals(grade.getStatus())
                        && Objects.equals(attempt.getId(), grade.getCountedAttemptId());
        if (releasedCountedAttempt) {
            quizAccessService.requireInstructor(courseId, userId);
            if (body.getReason() == null || body.getReason().isBlank()) {
                throw new ApiException(ErrorType.BAD_REQUEST, "Reason required to change released score");
            }
        }
        if (body.getScore().compareTo(BigDecimal.ZERO) < 0
                || body.getScore().compareTo(question.getPoints()) > 0) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Score out of range");
        }
        BigDecimal before = answer.getScore();
        String feedbackBefore = answer.getFeedback();
        boolean visibleChanged = !scoresEqual(before, body.getScore())
                || !Objects.equals(feedbackBefore, body.getFeedback());

        var now = quizTimeSupport.nowUtc();
        answer.setScore(body.getScore());
        answer.setPendingManual(false);
        answer.setFeedback(body.getFeedback());
        answer.setGradedBy(userId);
        answer.setGradedAt(now);
        answer.setUpdatedAt(now);
        quizAttemptAnswerMapper.updateScore(answer);
        recalcAttempt(attemptId);

        if (visibleChanged) {
            QuizScoreAudit audit = new QuizScoreAudit();
            audit.setQuizId(quizId);
            audit.setAttemptId(attemptId);
            audit.setQuestionId(questionId);
            audit.setActorUserId(userId);
            audit.setReason(body.getReason());
            audit.setScoreBefore(before);
            audit.setScoreAfter(body.getScore());
            audit.setCreatedAt(now);
            quizScoreAuditMapper.insert(audit);

            if (releasedCountedAttempt) {
                dispatchQuizGradeCorrected(courseId, quizId, attempt.getUserId(), audit.getId(), now);
            }
        }
        quizAuditService.log(courseId, quizId, attemptId, userId, "ANSWER_GRADED", body.getReason(), null);
    }

    @Transactional
    public void release(Integer courseId, Integer quizId, Integer userId, ReleaseGradesRequest body) {
        quizAccessService.requireReleaseWritable(courseId, userId);
        validateUserIds(body);

        List<Integer> filterUserIds = body == null ? null : body.getUserIds();
        Set<Integer> filterSet = filterUserIds == null ? null : new HashSet<>(filterUserIds);
        List<Integer> enteredUserIds = new ArrayList<>();
        List<QuizGrade> grades = quizGradeMapper.selectByQuizId(quizId);
        if (grades != null) {
            for (QuizGrade g : grades) {
                if (g == null || g.getUserId() == null) {
                    continue;
                }
                if (!QuizConstants.GRADE_ENTERED.equals(g.getStatus())) {
                    continue;
                }
                if (filterSet != null && !filterSet.contains(g.getUserId())) {
                    continue;
                }
                enteredUserIds.add(g.getUserId());
            }
        }

        quizGradeMapper.release(quizId, filterUserIds, userId);
        Integer auditId = quizAuditService.log(courseId, quizId, null, userId, "GRADES_RELEASED", null, null);

        if (!enteredUserIds.isEmpty() && auditId != null) {
            Course course = courseMapper.selectById(courseId);
            Quiz quiz = quizMapper.selectById(quizId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(course, enteredUserIds);
            if (course != null && course.getTenantId() != null && quiz != null && !recipients.isEmpty()) {
                NotificationDispatchPayload payload = new NotificationDispatchPayload();
                payload.setTenantId(course.getTenantId());
                payload.setCourseId(courseId);
                payload.setNotificationType(NotificationType.QUIZ_GRADE_RELEASED);
                payload.setMessage(notificationMessageFactory.quizGradeReleased(quiz.getTitle()));
                payload.setSubjectType(SubjectType.QUIZ);
                payload.setSubjectId(quizId);
                payload.setEventKey("release:" + auditId);
                payload.setDeepLink("/courses/" + courseId + "/quizzes/" + quizId + "/my-grade");
                payload.setRecipientIds(recipients);
                payload.setRecipientMode(RecipientMode.EXPLICIT);
                payload.setCreatedAt(notificationTimeSupport.nowUtc());
                payload.setTemplateVars(quizVars(course, quiz, payload.getDeepLink()));
                notificationCommitHook.afterCommitDispatch(payload);
            }
        }
    }

    @Transactional
    public void retract(Integer courseId, Integer quizId, Integer userId, ReleaseGradesRequest body) {
        quizAccessService.requireReleaseWritable(courseId, userId);
        validateUserIds(body);
        quizGradeMapper.retract(quizId, body == null ? null : body.getUserIds());
        quizAuditService.log(courseId, quizId, null, userId, "GRADES_RETRACTED", null, null);
    }

    private void dispatchQuizGradeCorrected(Integer courseId, Integer quizId, Integer studentUserId,
                                            Integer scoreAuditId, LocalDateTime createdAt) {
        if (studentUserId == null || scoreAuditId == null) {
            return;
        }
        Course course = courseMapper.selectById(courseId);
        Quiz quiz = quizMapper.selectById(quizId);
        List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(
                course, List.of(studentUserId));
        if (course == null || course.getTenantId() == null || quiz == null || recipients.isEmpty()) {
            return;
        }
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setTenantId(course.getTenantId());
        payload.setCourseId(courseId);
        payload.setNotificationType(NotificationType.QUIZ_GRADE_CORRECTED);
        payload.setMessage(notificationMessageFactory.quizGradeCorrected(quiz.getTitle()));
        payload.setSubjectType(SubjectType.QUIZ);
        payload.setSubjectId(quizId);
        payload.setEventKey("correct:" + scoreAuditId);
        payload.setDeepLink("/courses/" + courseId + "/quizzes/" + quizId + "/my-grade");
        payload.setRecipientIds(recipients);
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setCreatedAt(createdAt != null ? createdAt : notificationTimeSupport.nowUtc());
        payload.setTemplateVars(quizVars(course, quiz, payload.getDeepLink()));
        notificationCommitHook.afterCommitDispatch(payload);
    }

    private Map<String, String> quizVars(Course course, Quiz quiz, String deepLink) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("courseCode", course.getCourseCode() == null ? "" : course.getCourseCode());
        vars.put("courseTitle", course.getTitle() == null ? "" : course.getTitle());
        vars.put("quizTitle", quiz.getTitle() == null ? "" : quiz.getTitle());
        vars.put("deepLink", deepLink);
        return vars;
    }

    private void validateUserIds(ReleaseGradesRequest body) {
        if (body != null && body.getUserIds() != null && body.getUserIds().isEmpty()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "userIds cannot be empty array");
        }
    }

    private int countManualIncomplete(Integer quizId) {
        int count = 0;
        List<QuizAttempt> attempts = quizAttemptMapper.selectByQuizId(quizId, null, 0, 10000);
        for (QuizAttempt a : attempts) {
            if (QuizConstants.ATTEMPT_SUBMITTED.equals(a.getStatus())
                    && !Boolean.TRUE.equals(a.getManualGradingComplete())) {
                count++;
            }
        }
        return count;
    }

    private void recalcAttempt(Integer attemptId) {
        QuizAttempt attempt = quizAttemptMapper.selectById(attemptId);
        List<QuizAttemptAnswer> answers = quizAttemptAnswerMapper.selectByAttemptId(attemptId);
        BigDecimal manual = BigDecimal.ZERO;
        BigDecimal auto = attempt.getAutoScore() == null ? BigDecimal.ZERO : attempt.getAutoScore();
        boolean pending = false;
        for (QuizAttemptAnswer a : answers) {
            if (Boolean.TRUE.equals(a.getPendingManual())) {
                pending = true;
            } else if (a.getScore() != null) {
                if (QuizConstants.TYPE_SHORT_ANSWER.equals(
                        quizQuestionMapper.selectById(a.getQuestionId()).getType())) {
                    manual = manual.add(a.getScore());
                }
            }
        }
        attempt.setManualScore(manual);
        attempt.setTotalScore(auto.add(manual));
        attempt.setManualGradingComplete(!pending);
        attempt.setUpdatedAt(quizTimeSupport.nowUtc());
        quizAttemptMapper.updateSubmitted(attempt);
    }

    static boolean scoresEqual(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }
}
