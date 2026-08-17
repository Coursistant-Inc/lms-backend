package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.interaction.notification.event.NotificationEventRelayWorker;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;

/**
 * Pins NotificationRecipientResolver catch-and-empty: a retryable mapper failure must not
 * let Relay mark the outbox DONE as if there were zero legitimate recipients.
 */
class RecipientResolutionFailureIT extends NotificationPhase1SpringITBase {

    @Autowired
    private NotificationEventRelayWorker relayWorker;

    @MockitoSpyBean
    private EnrollmentMapper enrollmentMapper;

    @Test
    void processOne_enrollmentMapperThrows_marksRetryableNotDone() {
        int instructorId = insertInstructor();
        int studentId = insertUser("resolve-fail@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollStudent(courseId, studentId);
        String eventId = uuid();
        long outboxId = insertOutbox(eventId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 44,
                "ann-" + eventId, "COURSE_ACTIVE_STUDENTS", "PENDING", courseId);

        doThrow(new IllegalStateException("injected-enrollment-db-failure"))
                .when(enrollmentMapper).selectActiveStudentsByCourseId(anyInt());

        relayWorker.processOne(outboxId);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_event_outbox WHERE id = ?", String.class, outboxId);
        assertEquals("FAILED_RETRYABLE", status);
    }
}
