package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.service.NotificationDispatcher;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Best-effort assignment notifications. Tier-1 delivery writes via interaction
 * {@link NotificationDispatcher}; other events remain log-only. Notifications run
 * <em>after</em> the transaction commits and must never fail the business operation.
 */
@Service
public class AssignmentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentNotificationService.class);

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private NotificationMessageFactory notificationMessageFactory;

    @Resource
    private NotificationDispatcher notificationDispatcher;

    @Resource
    private NotificationTimeSupport notificationTimeSupport;

    /**
     * Runs {@code action} once the current transaction commits, swallowing any failure.
     * Executes immediately when there is no active transaction.
     */
    public void afterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(action);
                }
            });
        } else {
            runSafely(action);
        }
    }

    public void notifyAssignmentPublished(Assignment assignment, List<Integer> studentUserIds) {
        runSafely(() -> {
            if (assignment == null || assignment.getId() == null || assignment.getCourseId() == null) {
                return;
            }
            Course course = courseMapper.selectById(assignment.getCourseId());
            if (course == null || course.getTenantId() == null) {
                return;
            }
            NotificationDispatchPayload payload = new NotificationDispatchPayload();
            payload.setTenantId(course.getTenantId());
            payload.setCourseId(assignment.getCourseId());
            payload.setNotificationType(NotificationType.ASSIGNMENT_PUBLISHED);
            payload.setMessage(notificationMessageFactory.assignmentPublished(assignment.getTitle()));
            payload.setSubjectType(SubjectType.ASSIGNMENT);
            payload.setSubjectId(assignment.getId());
            payload.setEventKey("published");
            payload.setDeepLink("/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId());
            payload.setRecipientIds(copyRecipients(studentUserIds));
            payload.setCreatedAt(notificationTimeSupport.nowUtc());
            notificationDispatcher.dispatchAsync(payload);
        });
    }

    public void notifyDueDateChanged(Assignment assignment, LocalDateTime previousDueAt, List<Integer> studentUserIds) {
        runSafely(() -> log.info("Notify due date changed: courseId={}, assignmentId={}, from={}, to={}, recipients={}",
                assignment.getCourseId(), assignment.getId(), previousDueAt, assignment.getDueAt(), size(studentUserIds)));
    }

    public void notifySubmissionReceived(Assignment assignment, Integer studentUserId, Integer versionNo,
                                         LocalDateTime submittedAt) {
        runSafely(() -> log.info("Notify submission receipt: courseId={}, assignmentId={}, userId={}, versionNo={}, submittedAt={}",
                assignment.getCourseId(), assignment.getId(), studentUserId, versionNo, submittedAt));
    }

    public void notifyGroupSubmissionReplaced(Assignment assignment, Integer groupId, Integer submitterUserId,
                                              Integer versionNo) {
        runSafely(() -> log.info(
                "Notify GROUP_SUBMISSION_REPLACED: courseId={}, assignmentId={}, groupId={}, submitterUserId={}, versionNo={}",
                assignment.getCourseId(), assignment.getId(), groupId, submitterUserId, versionNo));
    }

    public void notifyGradesReleased(Assignment assignment, List<Integer> studentUserIds, Integer auditId) {
        runSafely(() -> {
            if (assignment == null || assignment.getId() == null || assignment.getCourseId() == null || auditId == null) {
                return;
            }
            Course course = courseMapper.selectById(assignment.getCourseId());
            if (course == null || course.getTenantId() == null) {
                return;
            }
            NotificationDispatchPayload payload = new NotificationDispatchPayload();
            payload.setTenantId(course.getTenantId());
            payload.setCourseId(assignment.getCourseId());
            payload.setNotificationType(NotificationType.ASSIGNMENT_GRADE_RELEASED);
            payload.setMessage(notificationMessageFactory.assignmentGradeReleased(assignment.getTitle()));
            payload.setSubjectType(SubjectType.ASSIGNMENT);
            payload.setSubjectId(assignment.getId());
            payload.setEventKey("release:" + auditId);
            payload.setDeepLink("/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId() + "/my-grade");
            payload.setRecipientIds(copyRecipients(studentUserIds));
            payload.setCreatedAt(notificationTimeSupport.nowUtc());
            notificationDispatcher.dispatchAsync(payload);
        });
    }

    /**
     * Post-release score/feedback/annotated-file correction. Body must not include the numeric score.
     */
    public void notifyGradeCorrectedAfterRelease(Assignment assignment, List<Integer> studentUserIds, Integer auditId) {
        runSafely(() -> {
            if (assignment == null || assignment.getId() == null || assignment.getCourseId() == null || auditId == null) {
                return;
            }
            Course course = courseMapper.selectById(assignment.getCourseId());
            if (course == null || course.getTenantId() == null) {
                return;
            }
            NotificationDispatchPayload payload = new NotificationDispatchPayload();
            payload.setTenantId(course.getTenantId());
            payload.setCourseId(assignment.getCourseId());
            payload.setNotificationType(NotificationType.ASSIGNMENT_GRADE_CORRECTED);
            payload.setMessage(notificationMessageFactory.assignmentGradeCorrected(assignment.getTitle()));
            payload.setSubjectType(SubjectType.ASSIGNMENT);
            payload.setSubjectId(assignment.getId());
            payload.setEventKey("correct:" + auditId);
            payload.setDeepLink("/courses/" + assignment.getCourseId() + "/assignments/" + assignment.getId() + "/my-grade");
            payload.setRecipientIds(copyRecipients(studentUserIds));
            payload.setCreatedAt(notificationTimeSupport.nowUtc());
            notificationDispatcher.dispatchAsync(payload);
        });
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("Assignment notification failed (ignored): {}", e.getMessage());
        }
    }

    private List<Integer> copyRecipients(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }

    private int size(List<Integer> values) {
        return values == null ? 0 : values.size();
    }
}
