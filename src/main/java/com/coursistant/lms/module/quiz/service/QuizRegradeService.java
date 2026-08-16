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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    @Resource
    private QuizMapper quizMapper;
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

        Set<Integer> affectedReleasedUsers = new LinkedHashSet<>();
        List<QuizQuestionOption> options = quizQuestionOptionMapper.selectInstructorByQuestionId(questionId);
        for (QuizAttemptAnswer answer : quizAttemptAnswerMapper.selectByQuestionId(questionId)) {
            QuizAttempt attempt = quizAttemptMapper.selectById(answer.getAttemptId());
            if (attempt == null || !QuizConstants.ATTEMPT_SUBMITTED.equals(attempt.getStatus())) {
                continue;
            }
            if (QuizConstants.TYPE_SHORT_ANSWER.equals(question.getType())) {
                continue;
            }
            BigDecimal oldScore = answer.getScore();
            List<Integer> selected = quizJsonUtil.parseOptionIds(answer.getSelectedOptionIdsJson());
            BigDecimal score = quizScoringService.scoreObjective(question, selected, options);
            boolean scoreChanged = !QuizGradingService.scoresEqual(oldScore, score);
            answer.setScore(score);
            answer.setPendingManual(false);
            answer.setUpdatedAt(quizTimeSupport.nowUtc());
            quizAttemptAnswerMapper.updateScore(answer);
            recalcAttemptTotals(attempt.getId());
            QuizGrade grade = quizGradeMapper.selectByQuizIdAndUserId(quizId, attempt.getUserId());
            if (grade != null) {
                quizGradeMapper.incrementVersion(grade.getId());
                boolean releasedCountedAttempt =
                        QuizConstants.GRADE_RELEASED.equals(grade.getStatus())
                                && Objects.equals(attempt.getId(), grade.getCountedAttemptId());
                if (scoreChanged && releasedCountedAttempt) {
                    affectedReleasedUsers.add(attempt.getUserId());
                }
            }
        }

        quizAuditService.log(courseId, quizId, null, userId, "ANSWER_KEY_UPDATED", body.getReason(),
                Map.of("questionId", questionId));
        quizAuditService.log(courseId, quizId, null, userId, "SCORES_RECALCULATED", body.getReason(), null);

        if (!affectedReleasedUsers.isEmpty()) {
            Course course = courseMapper.selectById(courseId);
            Quiz quiz = quizMapper.selectById(quizId);
            List<Integer> recipients = notificationRecipientResolver.filterCandidateRecipients(
                    course, new ArrayList<>(affectedReleasedUsers));
            if (course != null && course.getTenantId() != null && quiz != null && !recipients.isEmpty()) {
                NotificationDispatchPayload payload = new NotificationDispatchPayload();
                payload.setTenantId(course.getTenantId());
                payload.setCourseId(courseId);
                payload.setNotificationType(NotificationType.QUIZ_GRADE_CORRECTED);
                payload.setMessage(notificationMessageFactory.quizGradeCorrected(quiz.getTitle()));
                payload.setSubjectType(SubjectType.QUIZ);
                payload.setSubjectId(quizId);
                payload.setEventKey("correct:regrade:" + questionId + ":" + question.getVersion());
                payload.setDeepLink("/courses/" + courseId + "/quizzes/" + quizId + "/my-grade");
                payload.setRecipientIds(recipients);
                payload.setRecipientMode(RecipientMode.EXPLICIT);
                payload.setCreatedAt(notificationTimeSupport.nowUtc());
                Map<String, String> vars = new LinkedHashMap<>();
                vars.put("courseCode", course.getCourseCode() == null ? "" : course.getCourseCode());
                vars.put("courseTitle", course.getTitle() == null ? "" : course.getTitle());
                vars.put("quizTitle", quiz.getTitle() == null ? "" : quiz.getTitle());
                vars.put("deepLink", payload.getDeepLink());
                payload.setTemplateVars(vars);
                notificationCommitHook.afterCommitDispatch(payload);
            }
        }

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
