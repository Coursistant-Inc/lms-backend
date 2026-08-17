package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.event.NotificationEventRelayWorker;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import com.coursistant.lms.module.interaction.notification.repository.NotificationDeliveryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

/**
 * Pins Relay/Fanout atomicity: in-app and email rows commit together, and a persist
 * failure rolls both back without marking the outbox DONE.
 */
class NotificationChannelPartialFailureIT extends NotificationPhase1SpringITBase {

    @Autowired
    private NotificationEventRelayWorker relayWorker;

    @MockitoSpyBean
    private NotificationDeliveryMapper deliveryMapper;

    @AfterEach
    void resetSpies() {
        Mockito.reset(deliveryMapper);
    }

    @Test
    void processOne_writesInAppAndImmediateEmail_withoutInAppDeliveryOrDuplicates() {
        SeededEvent seeded = seedGradeReleased();

        relayWorker.processOne(seeded.outboxId());
        relayWorker.processOne(seeded.outboxId());

        assertEquals("DONE", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_event_outbox WHERE id = ?", String.class, seeded.outboxId()));
        assertEquals(2, count("""
                SELECT COUNT(*) FROM user_notification
                WHERE notification_type = 'ASSIGNMENT_GRADE_RELEASED' AND subject_id = ?
                """, seeded.subjectId()));
        assertEquals(2, count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE event_id = ? AND channel = 'IMMEDIATE_EMAIL'
                """, seeded.eventId()));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE event_id = ? AND channel = 'IN_APP'
                """, seeded.eventId()));
    }

    @Test
    void processOne_emailPersistThrows_rollsBackInAppAndMarksRetryable() {
        SeededEvent seeded = seedGradeReleased();
        doThrow(new IllegalStateException("injected-email-persist-failure"))
                .when(deliveryMapper).upsertChunk(anyList());

        relayWorker.processOne(seeded.outboxId());

        assertEquals("FAILED_RETRYABLE", jdbcTemplate.queryForObject(
                "SELECT status FROM notification_event_outbox WHERE id = ?", String.class, seeded.outboxId()));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM user_notification
                WHERE notification_type = 'ASSIGNMENT_GRADE_RELEASED' AND subject_id = ?
                """, seeded.subjectId()));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_delivery WHERE event_id = ?
                """, seeded.eventId()));
    }

    private SeededEvent seedGradeReleased() {
        int instructorId = insertInstructor();
        int studentA = insertUser("partial-a-" + uuid() + "@example.com", true, "ACTIVE");
        int studentB = insertUser("partial-b-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollStudent(courseId, studentA);
        enrollStudent(courseId, studentB);
        String eventId = uuid();
        int subjectId = 9;
        long outboxId = insertOutbox(eventId, "ASSIGNMENT_GRADE_RELEASED", "ASSIGNMENT", subjectId,
                "grade-" + eventId, "EXPLICIT", "PENDING", courseId);
        jdbcTemplate.update(
                "INSERT INTO notification_event_recipient (outbox_id, recipient_user_id) VALUES (?, ?), (?, ?)",
                outboxId, studentA, outboxId, studentB);
        return new SeededEvent(outboxId, eventId, subjectId);
    }

    private record SeededEvent(long outboxId, String eventId, int subjectId) {
    }
}
