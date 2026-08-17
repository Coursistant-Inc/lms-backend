package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationEventKeys;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class QuizNotificationService {

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private NotificationMessageFactory notificationMessageFactory;

    @Resource
    private NotificationPublisher notificationPublisher;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    @Resource
    private NotificationRecipientResolver notificationRecipientResolver;

    @Resource
    private TenantTimezoneService tenantTimezoneService;

    public void recordQuizPublished(Quiz quiz, Integer actorUserId) {
        Course course = loadCourse(quiz);
        if (course == null) {
            return;
        }
        NotificationDispatchPayload payload = basePayload(course, quiz);
        payload.setNotificationType(NotificationType.QUIZ_PUBLISHED);
        payload.setMessage(notificationMessageFactory.quizPublished(quiz.getTitle()));
        payload.setEventKey(NotificationEventKeys.quizPublished(quiz.getId(), quiz.getPublicationVersion()));
        payload.setActorUserId(actorUserId);
        payload.setRecipientIds(notificationRecipientResolver.resolveForType(
                NotificationType.QUIZ_PUBLISHED, quiz.getCourseId(), actorUserId));
        payload.setTemplateVars(courseVars(course, quiz.getTitle(), payload.getDeepLink()));
        notificationPublisher.publishInTransaction(payload);
    }

    public void recordScheduleChanged(Quiz quiz, Integer actorUserId) {
        Course course = loadCourse(quiz);
        if (course == null) {
            return;
        }
        ZoneId zone = tenantTimezoneService.requireZoneForCourse(quiz.getCourseId());
        String window = notificationMessageFactory.formatWindow(quiz.getOpensAt(), quiz.getClosesAt(), zone);
        NotificationDispatchPayload payload = basePayload(course, quiz);
        payload.setNotificationType(NotificationType.QUIZ_SCHEDULE_CHANGED);
        payload.setMessage(notificationMessageFactory.quizScheduleChanged(quiz.getTitle(), window));
        payload.setEventKey(NotificationEventKeys.quizSchedule(quiz.getId(), quiz.getVersion()));
        payload.setActorUserId(actorUserId);
        payload.setRecipientIds(notificationRecipientResolver.resolveForType(
                NotificationType.QUIZ_SCHEDULE_CHANGED, quiz.getCourseId(), actorUserId));
        Map<String, String> vars = courseVars(course, quiz.getTitle(), payload.getDeepLink());
        vars.put("window", window);
        payload.setTemplateVars(vars);
        notificationPublisher.publishInTransaction(payload);
    }

    public void recordTimeLimitChanged(Quiz quiz, Integer actorUserId) {
        Course course = loadCourse(quiz);
        if (course == null) {
            return;
        }
        NotificationDispatchPayload payload = basePayload(course, quiz);
        payload.setNotificationType(NotificationType.QUIZ_TIME_LIMIT_CHANGED);
        payload.setMessage(notificationMessageFactory.quizTimeLimitChanged(quiz.getTitle()));
        payload.setEventKey(NotificationEventKeys.quizTimeLimit(quiz.getId(), quiz.getVersion()));
        payload.setActorUserId(actorUserId);
        payload.setRecipientIds(notificationRecipientResolver.resolveForType(
                NotificationType.QUIZ_TIME_LIMIT_CHANGED, quiz.getCourseId(), actorUserId));
        payload.setTemplateVars(courseVars(course, quiz.getTitle(), payload.getDeepLink()));
        notificationPublisher.publishInTransaction(payload);
    }

    private NotificationDispatchPayload basePayload(Course course, Quiz quiz) {
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setTenantId(course.getTenantId());
        payload.setCourseId(quiz.getCourseId());
        payload.setSubjectType(SubjectType.QUIZ);
        payload.setSubjectId(quiz.getId());
        payload.setDeepLink("/courses/" + quiz.getCourseId() + "/quizzes/" + quiz.getId());
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setCreatedAt(notificationTimeSupport.nowUtc());
        return payload;
    }

    private Course loadCourse(Quiz quiz) {
        if (quiz == null || quiz.getId() == null || quiz.getCourseId() == null) {
            return null;
        }
        Course course = courseMapper.selectById(quiz.getCourseId());
        if (course == null || course.getTenantId() == null || course.getArchivedAt() != null) {
            return null;
        }
        return course;
    }

    private Map<String, String> courseVars(Course course, String title, String deepLink) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("courseCode", course.getCourseCode() == null ? "" : course.getCourseCode());
        vars.put("courseTitle", course.getTitle() == null ? "" : course.getTitle());
        vars.put("quizTitle", title == null ? "" : title);
        vars.put("deepLink", deepLink);
        return vars;
    }
}
