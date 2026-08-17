package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.event.NotificationEventRelayWorker;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins Relay COURSE_ACTIVE_STUDENTS re-checking archivedAt: an event committed while the
 * course was active must still fan-out after a later archive.
 */
class ArchivedAfterCommitRelayIT extends NotificationPhase1SpringITBase {

    @Autowired
    private NotificationEventRelayWorker relayWorker;

    @Test
    void processOne_courseArchivedAfterCommit_stillFansOutActiveStudents() {
        int instructorId = insertInstructor();
        int studentA = insertUser("arch-a@example.com", true, "ACTIVE");
        int studentB = insertUser("arch-b@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollStudent(courseId, studentA);
        enrollStudent(courseId, studentB);
        String eventId = uuid();
        long outboxId = insertOutbox(eventId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 88,
                "ann-" + eventId, "COURSE_ACTIVE_STUDENTS", "PENDING", courseId);

        archiveCourse(courseId);
        relayWorker.processOne(outboxId);

        int inApp = count("""
                SELECT COUNT(*) FROM user_notification
                WHERE course_id = ? AND notification_type = 'ANNOUNCEMENT_POSTED'
                """, courseId);
        assertEquals(2, inApp, "archive after commit must not suppress snapshot-time active students");
    }
}
