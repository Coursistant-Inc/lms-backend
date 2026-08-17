package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.service.AssignmentNotificationService;
import com.coursistant.lms.module.course.content.week.repository.CourseWeekMapper;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase2SpringITBase;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.quiz.service.QuizNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NotificationPublicationScheduleIT extends NotificationPhase2SpringITBase {

    @Autowired private AssignmentMapper assignmentMapper;
    @Autowired private QuizMapper quizMapper;
    @Autowired private CourseWeekMapper courseWeekMapper;
    @Autowired private AssignmentNotificationService assignmentNotificationService;
    @Autowired private QuizNotificationService quizNotificationService;

    @Test
    void publishRepublishAndScheduleChanges_useCanonicalVersionsAndDedupe() {
        int instructorId = insertInstructor();
        int studentId = insertUser("pub-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollStudent(courseId, studentId);

        int assignmentId = insertAssignment(courseId, instructorId, "Draft");
        assertEquals(1, assignmentMapper.publishAndIncrementPublicationVersion(assignmentId));
        Assignment published = assignmentMapper.selectById(assignmentId);
        assertEquals("Published", published.getState());
        assertEquals(1, published.getPublicationVersion());
        transactionTemplate.executeWithoutResult(status ->
                assignmentNotificationService.recordAssignmentPublished(published, instructorId));
        assertEquals(0, assignmentMapper.publishAndIncrementPublicationVersion(assignmentId));

        jdbcTemplate.update("UPDATE assignment SET state = 'Draft' WHERE id = ?", assignmentId);
        assertEquals(1, assignmentMapper.publishAndIncrementPublicationVersion(assignmentId));
        Assignment republished = assignmentMapper.selectById(assignmentId);
        assertEquals(2, republished.getPublicationVersion());
        transactionTemplate.executeWithoutResult(status ->
                assignmentNotificationService.recordAssignmentPublished(republished, instructorId));

        assertEquals(2, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_PUBLISHED' AND subject_id = ?
                """, assignmentId));

        jdbcTemplate.update("UPDATE assignment SET due_at = '2026-09-02 12:00:00' WHERE id = ?", assignmentId);
        assertEquals(1, assignmentMapper.incrementScheduleVersion(assignmentId));
        Assignment scheduled = assignmentMapper.selectById(assignmentId);
        assertEquals(1, scheduled.getScheduleVersion());
        transactionTemplate.executeWithoutResult(status ->
                assignmentNotificationService.recordScheduleChanged(scheduled, instructorId));
        transactionTemplate.executeWithoutResult(status ->
                assignmentNotificationService.recordScheduleChanged(scheduled, instructorId));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_SCHEDULE_CHANGED' AND subject_id = ?
                """, assignmentId));

        int quizId = insertQuiz(courseId, instructorId, "Draft", 1);
        assertEquals(1, quizMapper.publishAndIncrementPublicationVersion(quizId, 1));
        Quiz quiz = quizMapper.selectById(quizId);
        assertEquals("Published", quiz.getState());
        assertEquals(1, quiz.getPublicationVersion());
        assertEquals(2, quiz.getVersion());
        transactionTemplate.executeWithoutResult(status ->
                quizNotificationService.recordQuizPublished(quiz, instructorId));
        assertEquals(0, quizMapper.publishAndIncrementPublicationVersion(quizId, 1));

        jdbcTemplate.update("""
                UPDATE quiz SET opens_at = '2026-09-01 11:00:00', version = version + 1
                WHERE id = ?
                """, quizId);
        Quiz scheduleQuiz = quizMapper.selectById(quizId);
        transactionTemplate.executeWithoutResult(status ->
                quizNotificationService.recordScheduleChanged(scheduleQuiz, instructorId));
        jdbcTemplate.update("UPDATE quiz SET time_limit_seconds = 1800 WHERE id = ?", quizId);
        Quiz limitQuiz = quizMapper.selectById(quizId);
        transactionTemplate.executeWithoutResult(status ->
                quizNotificationService.recordTimeLimitChanged(limitQuiz, instructorId));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'QUIZ_SCHEDULE_CHANGED' AND subject_id = ?
                """, quizId));
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'QUIZ_TIME_LIMIT_CHANGED' AND subject_id = ?
                """, quizId));
        String scheduleKey = jdbcTemplate.queryForObject(
                "SELECT event_key FROM notification_event_outbox WHERE notification_type = 'QUIZ_SCHEDULE_CHANGED'",
                String.class);
        String limitKey = jdbcTemplate.queryForObject(
                "SELECT event_key FROM notification_event_outbox WHERE notification_type = 'QUIZ_TIME_LIMIT_CHANGED'",
                String.class);
        assertNotEquals(scheduleKey, limitKey);

        int weekId = insertWeek(courseId, "Draft");
        assertEquals(1, courseWeekMapper.publishAndIncrementPublicationVersion(weekId));
        assertEquals(0, courseWeekMapper.publishAndIncrementPublicationVersion(weekId));
        jdbcTemplate.update("UPDATE course_week SET state = 'Draft' WHERE id = ?", weekId);
        assertEquals(1, courseWeekMapper.publishAndIncrementPublicationVersion(weekId));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT publication_version FROM course_week WHERE id = ?", Integer.class, weekId));
    }

    @Test
    void unpublishedAssignment_doesNotNeedScheduleEventFromNoOpTimes() {
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        int assignmentId = insertAssignment(courseId, instructorId, "Draft");
        Assignment draft = assignmentMapper.selectById(assignmentId);
        LocalDateTime due = draft.getDueAt();
        jdbcTemplate.update("UPDATE assignment SET due_at = ? WHERE id = ?", due, assignmentId);
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_SCHEDULE_CHANGED' AND subject_id = ?
                """, assignmentId));
    }
}
