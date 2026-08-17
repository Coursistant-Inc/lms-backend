package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.service.AssignmentNotificationService;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationEventRelayWorker;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase2SpringITBase;
import com.coursistant.lms.module.interaction.notification.service.NotificationFanoutService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

class NotificationPhase2ChannelIT extends NotificationPhase2SpringITBase {

    @Autowired private NotificationEventRelayWorker relayWorker;
    @Autowired private AssignmentNotificationService assignmentNotificationService;
    @Autowired private AssignmentMapper assignmentMapper;
    @MockitoSpyBean private NotificationFanoutService fanoutService;

    @AfterEach
    void resetSpies() {
        Mockito.reset(fanoutService);
    }

    @Test
    void nineNewTypes_areInAppPlusDigest_andPhase1ImmediateRemains() {
        int instructorId = insertInstructor();
        int studentId = insertUser("ch-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollStudent(courseId, studentId);
        List<Integer> recipients = List.of(studentId);

        publishExplicit(courseId, NotificationType.WEEK_PUBLISHED, SubjectType.WEEK, 1,
                "week:1:publication:1", instructorId, recipients);
        publishExplicit(courseId, NotificationType.ASSIGNMENT_SCHEDULE_CHANGED, SubjectType.ASSIGNMENT, 2,
                "assignment:2:schedule:1", instructorId, recipients);
        publishExplicit(courseId, NotificationType.QUIZ_PUBLISHED, SubjectType.QUIZ, 3,
                "quiz:3:publication:1", instructorId, recipients);
        publishExplicit(courseId, NotificationType.QUIZ_SCHEDULE_CHANGED, SubjectType.QUIZ, 3,
                "quiz:3:schedule:2", instructorId, recipients);
        publishExplicit(courseId, NotificationType.QUIZ_TIME_LIMIT_CHANGED, SubjectType.QUIZ, 3,
                "quiz:3:time-limit:2", instructorId, recipients);
        publishExplicit(courseId, NotificationType.COURSE_EVENT_CREATED, SubjectType.COURSE_EVENT, 4,
                "course-event:4:created", instructorId, recipients);
        publishExplicit(courseId, NotificationType.GROUP_MEMBER_ADDED, SubjectType.GROUP_SET, 5,
                "group-membership-change:1:added:target", instructorId, recipients);
        publishExplicit(courseId, NotificationType.GROUP_MEMBER_REMOVED, SubjectType.GROUP_SET, 5,
                "group-membership-change:2:removed:target", instructorId, recipients);
        publishExplicit(courseId, NotificationType.GROUP_MEMBER_MOVED, SubjectType.GROUP_SET, 5,
                "group-membership-change:3:moved:target", instructorId, recipients);

        assertEquals(9, count("""
                SELECT COUNT(*) FROM user_notification WHERE recipient_user_id = ?
                """, studentId));
        assertEquals(9, count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE recipient_user_id = ? AND channel = 'DAILY_DIGEST'
                """, studentId));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE recipient_user_id = ? AND channel = 'IMMEDIATE_EMAIL'
                  AND notification_type IN (
                    'WEEK_PUBLISHED','ASSIGNMENT_SCHEDULE_CHANGED','QUIZ_PUBLISHED',
                    'QUIZ_SCHEDULE_CHANGED','QUIZ_TIME_LIMIT_CHANGED','COURSE_EVENT_CREATED',
                    'GROUP_MEMBER_ADDED','GROUP_MEMBER_REMOVED','GROUP_MEMBER_MOVED')
                """, studentId));

        publishExplicit(courseId, NotificationType.ASSIGNMENT_GRADE_RELEASED, SubjectType.ASSIGNMENT, 9,
                "release:77", instructorId, recipients);
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE recipient_user_id = ? AND channel = 'IMMEDIATE_EMAIL'
                  AND notification_type = 'ASSIGNMENT_GRADE_RELEASED'
                """, studentId));
    }

    @Test
    void archivedCourse_doesNotWriteNewOutbox_andRelayFailureDoesNotRollbackBusiness() {
        int instructorId = insertInstructor();
        int studentId = insertUser("ch2-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollStudent(courseId, studentId);
        int assignmentId = insertAssignment(courseId, instructorId, "Published");
        jdbcTemplate.update("UPDATE assignment SET publication_version = 1 WHERE id = ?", assignmentId);

        archiveCourse(courseId);
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        transactionTemplate.executeWithoutResult(status ->
                assignmentNotificationService.recordAssignmentPublished(assignment, instructorId));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_PUBLISHED' AND subject_id = ?
                """, assignmentId));

        jdbcTemplate.update("UPDATE course SET archived_at = NULL, state = 'Active' WHERE id = ?", courseId);
        String eventId = uuid();
        long outboxId = insertOutbox(eventId, "ASSIGNMENT_PUBLISHED", "ASSIGNMENT", assignmentId,
                "assignment:" + assignmentId + ":publication:1", "EXPLICIT", "PENDING", courseId);
        jdbcTemplate.update(
                "INSERT INTO notification_event_recipient (outbox_id, recipient_user_id) VALUES (?, ?)",
                outboxId, studentId);
        doThrow(new IllegalStateException("injected-fanout-failure"))
                .when(fanoutService).persist(any(), anyList());
        relayWorker.processOne(outboxId);
        assertEquals(1, count("SELECT COUNT(*) FROM assignment WHERE id = ?", assignmentId));
        assertEquals("FAILED_RETRYABLE", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_event_outbox WHERE id = ?", String.class, outboxId));

        Mockito.reset(fanoutService);
        jdbcTemplate.update("""
                UPDATE notification_event_outbox
                SET next_attempt_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE)
                WHERE id = ?
                """, outboxId);
        relayWorker.processOne(outboxId);
        assertEquals("DONE", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_event_outbox WHERE id = ?", String.class, outboxId));
    }
}
