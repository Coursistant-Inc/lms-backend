package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentNotificationService {

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private NotificationMessageFactory notificationMessageFactory;

    @Resource
    private NotificationPublisher notificationPublisher;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    public void recordAssignmentPublished(Assignment assignment) {
        Course course = loadCourse(assignment);
        if (course == null) {
            return;
        }
        NotificationDispatchPayload payload = basePayload(course, assignment);
        payload.setNotificationType(NotificationType.ASSIGNMENT_PUBLISHED);
        payload.setMessage(notificationMessageFactory.assignmentPublished(assignment.getTitle()));
        payload.setSubjectType(SubjectType.ASSIGNMENT);
        payload.setSubjectId(assignment.getId());
        payload.setEventKey("published");
        payload.setDeepLink("/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId());
        payload.setRecipientMode(RecipientMode.COURSE_ACTIVE_STUDENTS);
        payload.setTemplateVars(courseVars(course, assignment.getTitle(), payload.getDeepLink()));
        notificationPublisher.publishInTransaction(payload);
    }

    public void recordSubmissionReceived(Assignment assignment, List<Integer> recipientIds,
                                         Integer submissionId, Integer versionNo, Integer submissionVersionId,
                                         LocalDateTime submittedAt) {
        Course course = loadCourse(assignment);
        if (course == null || recipientIds == null || recipientIds.isEmpty() || submissionVersionId == null) {
            return;
        }
        String deepLink = "/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId()
                + "/my-submission";
        NotificationDispatchPayload payload = basePayload(course, assignment);
        payload.setNotificationType(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED);
        payload.setMessage(notificationMessageFactory.submissionReceived(assignment.getTitle(), submittedAt));
        payload.setSubjectType(SubjectType.ASSIGNMENT_SUBMISSION);
        payload.setSubjectId(submissionId != null ? submissionId : assignment.getId());
        payload.setEventKey("submission:" + submissionVersionId);
        payload.setDeepLink(deepLink);
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setRecipientIds(copyRecipients(recipientIds));
        payload.setActorUserId(null);
        Map<String, String> vars = courseVars(course, assignment.getTitle(), deepLink);
        vars.put("submittedAt", submittedAt == null ? "" : submittedAt.toString());
        vars.put("versionNo", versionNo == null ? "" : String.valueOf(versionNo));
        payload.setTemplateVars(vars);
        notificationPublisher.publishInTransaction(payload);
    }

    public void recordSubmissionReceived(Assignment assignment, List<Integer> recipientIds,
                                         Integer submissionVersionId, Integer versionNo, LocalDateTime submittedAt) {
        recordSubmissionReceived(assignment, recipientIds, null, versionNo, submissionVersionId, submittedAt);
    }

    public void recordGradesReleased(Assignment assignment, List<Integer> studentUserIds, Integer auditId) {
        Course course = loadCourse(assignment);
        if (course == null || auditId == null) {
            return;
        }
        String deepLink = "/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId() + "/my-grade";
        NotificationDispatchPayload payload = basePayload(course, assignment);
        payload.setNotificationType(NotificationType.ASSIGNMENT_GRADE_RELEASED);
        payload.setMessage(notificationMessageFactory.assignmentGradeReleased(assignment.getTitle()));
        payload.setSubjectType(SubjectType.ASSIGNMENT);
        payload.setSubjectId(assignment.getId());
        payload.setEventKey("release:" + auditId);
        payload.setDeepLink(deepLink);
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setRecipientIds(copyRecipients(studentUserIds));
        payload.setTemplateVars(courseVars(course, assignment.getTitle(), deepLink));
        notificationPublisher.publishInTransaction(payload);
    }

    public void recordGradeCorrectedAfterRelease(Assignment assignment, List<Integer> studentUserIds, Integer auditId) {
        Course course = loadCourse(assignment);
        if (course == null || auditId == null) {
            return;
        }
        String deepLink = "/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId() + "/my-grade";
        NotificationDispatchPayload payload = basePayload(course, assignment);
        payload.setNotificationType(NotificationType.ASSIGNMENT_GRADE_CORRECTED);
        payload.setMessage(notificationMessageFactory.assignmentGradeCorrected(assignment.getTitle()));
        payload.setSubjectType(SubjectType.ASSIGNMENT);
        payload.setSubjectId(assignment.getId());
        payload.setEventKey("correct:" + auditId);
        payload.setDeepLink(deepLink);
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setRecipientIds(copyRecipients(studentUserIds));
        payload.setTemplateVars(courseVars(course, assignment.getTitle(), deepLink));
        notificationPublisher.publishInTransaction(payload);
    }

    private NotificationDispatchPayload basePayload(Course course, Assignment assignment) {
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setTenantId(course.getTenantId());
        payload.setCourseId(assignment.getCourseId());
        payload.setCreatedAt(notificationTimeSupport.nowUtc());
        return payload;
    }

    private Course loadCourse(Assignment assignment) {
        if (assignment == null || assignment.getId() == null || assignment.getCourseId() == null) {
            return null;
        }
        Course course = courseMapper.selectById(assignment.getCourseId());
        if (course == null || course.getTenantId() == null || course.getArchivedAt() != null) {
            return null;
        }
        return course;
    }

    private Map<String, String> courseVars(Course course, String title, String deepLink) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("courseCode", course.getCourseCode() == null ? "" : course.getCourseCode());
        vars.put("courseTitle", course.getTitle() == null ? "" : course.getTitle());
        vars.put("assignmentTitle", title == null ? "" : title);
        vars.put("deepLink", deepLink);
        return vars;
    }

    private List<Integer> copyRecipients(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }
}
