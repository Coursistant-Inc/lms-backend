package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase2SpringITBase;
import com.coursistant.lms.module.interaction.notification.service.NotificationRecipientResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationAudienceSnapshotIT extends NotificationPhase2SpringITBase {

    @Autowired
    private NotificationRecipientResolver resolver;

    @Test
    void announcementAndCourseEvent_includeAllActiveRoles_excludingActor() {
        int instructorId = insertInstructor();
        int taId = insertUser("ta-" + uuid() + "@example.com", true, "ACTIVE");
        int studentA = insertUser("snap-a-" + uuid() + "@example.com", true, "ACTIVE");
        int studentB = insertUser("snap-b-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollRole(courseId, instructorId, "Instructor");
        enrollRole(courseId, taId, "TA");
        enrollStudent(courseId, studentA);
        enrollStudent(courseId, studentB);

        List<Integer> announcement = resolver.resolveForType(
                NotificationType.ANNOUNCEMENT_POSTED, courseId, instructorId);
        assertTrue(announcement.contains(taId));
        assertTrue(announcement.contains(studentA));
        assertTrue(announcement.contains(studentB));
        assertFalse(announcement.contains(instructorId));

        List<Integer> event = resolver.resolveForType(
                NotificationType.COURSE_EVENT_CREATED, courseId, instructorId);
        assertEquals(announcement, event);
    }

    @Test
    void weekAssignmentQuiz_areStudentsOnly_andSnapshotSurvivesRosterChange() {
        int instructorId = insertInstructor();
        int taId = insertUser("ta2-" + uuid() + "@example.com", true, "ACTIVE");
        int studentA = insertUser("wk-a-" + uuid() + "@example.com", true, "ACTIVE");
        int studentB = insertUser("wk-b-" + uuid() + "@example.com", true, "ACTIVE");
        int studentC = insertUser("wk-c-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollRole(courseId, instructorId, "Instructor");
        enrollRole(courseId, taId, "TA");
        enrollStudent(courseId, studentA);
        enrollStudent(courseId, studentB);

        List<Integer> students = resolver.resolveForType(
                NotificationType.WEEK_PUBLISHED, courseId, instructorId);
        assertEquals(List.of(studentA, studentB), students);
        assertEquals(students, resolver.resolveForType(
                NotificationType.ASSIGNMENT_PUBLISHED, courseId, instructorId));
        assertEquals(students, resolver.resolveForType(
                NotificationType.QUIZ_PUBLISHED, courseId, instructorId));

        Long outboxId = publishExplicit(courseId, NotificationType.WEEK_PUBLISHED, SubjectType.WEEK,
                11, "week:11:publication:1", instructorId, students);
        assertEquals("EXPLICIT", outboxRecipientMode(outboxId));

        enrollStudent(courseId, studentC);
        deactivateEnrollment(courseId, studentB);

        assertEquals(2, countInApp("WEEK_PUBLISHED", courseId));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM user_notification
                WHERE notification_type = 'WEEK_PUBLISHED' AND recipient_user_id = ?
                """, studentA));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM user_notification
                WHERE notification_type = 'WEEK_PUBLISHED' AND recipient_user_id = ?
                """, studentB));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM user_notification
                WHERE notification_type = 'WEEK_PUBLISHED' AND recipient_user_id = ?
                """, studentC));
        assertEquals(2, count("""
                SELECT COUNT(*) FROM notification_event_recipient WHERE outbox_id = ?
                """, outboxId));
    }

    @Test
    void emptyEligibleRoster_stillWritesExplicitOutbox() {
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        enrollRole(courseId, instructorId, "Instructor");
        List<Integer> recipients = resolver.resolveForType(
                NotificationType.ASSIGNMENT_PUBLISHED, courseId, instructorId);
        assertTrue(recipients.isEmpty());
        Long outboxId = publishExplicit(courseId, NotificationType.ASSIGNMENT_PUBLISHED, SubjectType.ASSIGNMENT,
                9, "assignment:9:publication:1", instructorId, recipients);
        assertEquals("EXPLICIT", outboxRecipientMode(outboxId));
        assertEquals(0, count("SELECT COUNT(*) FROM notification_event_recipient WHERE outbox_id = ?", outboxId));
        assertEquals(0, countInApp("ASSIGNMENT_PUBLISHED", courseId));
    }
}
