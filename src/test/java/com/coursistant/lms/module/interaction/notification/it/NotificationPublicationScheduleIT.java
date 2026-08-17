package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.assignment.dto.PatchAssignmentRequest;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentRubricVersion;
import com.coursistant.lms.module.assignment.repository.AssignmentMapper;
import com.coursistant.lms.module.assignment.service.AssignmentResponseAssembler;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.module.course.content.week.repository.CourseWeekMapper;
import com.coursistant.lms.module.course.content.week.service.CourseWeekService;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase2SpringITBase;
import com.coursistant.lms.module.quiz.dto.authoring.PatchQuizRequest;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.quiz.service.QuizAuthoringService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

class NotificationPublicationScheduleIT extends NotificationPhase2SpringITBase {

    @Autowired private AssignmentService assignmentService;
    @Autowired private AssignmentMapper assignmentMapper;
    @Autowired private QuizAuthoringService quizAuthoringService;
    @Autowired private QuizMapper quizMapper;
    @Autowired private CourseWeekService courseWeekService;
    @Autowired private CourseWeekMapper courseWeekMapper;
    @MockitoSpyBean private AssignmentResponseAssembler assignmentResponseAssembler;

    @AfterEach
    void resetAssemblerSpy() {
        reset(assignmentResponseAssembler);
    }

    @Test
    void publishRepublishAndScheduleChanges_useCanonicalVersionsAndDedupe() {
        int instructorId = insertInstructor();
        int studentId = insertUser("pub-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollInstructor(courseId, instructorId);
        enrollStudent(courseId, studentId);

        int assignmentId = insertAssignment(courseId, instructorId, "Draft");
        assignmentService.publish(courseId, assignmentId, instructorId);
        Assignment published = assignmentMapper.selectById(assignmentId);
        assertEquals("Published", published.getState());
        assertEquals(1, published.getPublicationVersion());
        assignmentService.publish(courseId, assignmentId, instructorId);
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_PUBLISHED' AND subject_id = ?
                """, assignmentId));

        assignmentService.unpublish(courseId, assignmentId, instructorId);
        assignmentService.publish(courseId, assignmentId, instructorId);
        Assignment republished = assignmentMapper.selectById(assignmentId);
        assertEquals(2, republished.getPublicationVersion());
        assertEquals(2, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_PUBLISHED' AND subject_id = ?
                """, assignmentId));

        PatchAssignmentRequest laterDue = new PatchAssignmentRequest();
        laterDue.setDueAt(LocalDateTime.of(2026, 9, 20, 23, 59, 0));
        assignmentService.patch(courseId, assignmentId, instructorId, laterDue);
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_SCHEDULE_CHANGED' AND subject_id = ?
                """, assignmentId));
        assignmentService.patch(courseId, assignmentId, instructorId, laterDue);
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_SCHEDULE_CHANGED' AND subject_id = ?
                """, assignmentId));

        int quizId = insertQuiz(courseId, instructorId, "Draft", 1);
        insertShortAnswerQuestion(quizId);
        quizAuthoringService.publish(courseId, quizId, instructorId);
        Quiz quiz = quizMapper.selectById(quizId);
        assertEquals("Published", quiz.getState());
        assertEquals(1, quiz.getPublicationVersion());
        quizAuthoringService.publish(courseId, quizId, instructorId);
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'QUIZ_PUBLISHED' AND subject_id = ?
                """, quizId));

        PatchQuizRequest quizPatch = new PatchQuizRequest();
        quizPatch.setExpectedVersion(quizMapper.selectById(quizId).getVersion());
        quizPatch.setOpensAt(LocalDateTime.of(2026, 9, 1, 4, 0, 0));
        quizPatch.setTimeLimitSeconds(1800);
        quizAuthoringService.patch(courseId, quizId, instructorId, quizPatch);
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
        courseWeekService.publish(instructorActor(instructorId), courseId, weekId);
        courseWeekService.publish(instructorActor(instructorId), courseId, weekId);
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'WEEK_PUBLISHED' AND subject_id = ?
                """, weekId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT publication_version FROM course_week WHERE id = ?", Integer.class, weekId));
        courseWeekService.unpublish(instructorActor(instructorId), courseId, weekId);
        courseWeekService.publish(instructorActor(instructorId), courseId, weekId);
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT publication_version FROM course_week WHERE id = ?", Integer.class, weekId));
        assertEquals(2, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'WEEK_PUBLISHED' AND subject_id = ?
                """, weekId));
    }

    @Test
    void unpublishedAssignment_patchDoesNotEmitScheduleEvent() {
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        enrollInstructor(courseId, instructorId);
        int assignmentId = insertAssignment(courseId, instructorId, "Draft");
        PatchAssignmentRequest body = new PatchAssignmentRequest();
        body.setDueAt(LocalDateTime.of(2026, 9, 20, 23, 59, 0));
        assignmentService.patch(courseId, assignmentId, instructorId, body);
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_SCHEDULE_CHANGED' AND subject_id = ?
                """, assignmentId));
    }

    @Test
    void archivedCourse_publishThrowsAndWritesNoOutbox() {
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        enrollInstructor(courseId, instructorId);
        int assignmentId = insertAssignment(courseId, instructorId, "Draft");
        archiveCourse(courseId);

        ApiException ex = assertThrows(ApiException.class,
                () -> assignmentService.publish(courseId, assignmentId, instructorId));
        assertEquals(ErrorType.COURSE_ARCHIVED, ex.getErrorType());
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_PUBLISHED' AND subject_id = ?
                """, assignmentId));
        Assignment stillDraft = assignmentMapper.selectById(assignmentId);
        assertEquals("Draft", stillDraft.getState());
        assertEquals(0, stillDraft.getPublicationVersion());
    }

    @Test
    void publish_responseAssemblerThrow_rollsBackStateAuditAndOutbox() {
        int instructorId = insertInstructor();
        int studentId = insertUser("rb-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        enrollInstructor(courseId, instructorId);
        enrollStudent(courseId, studentId);
        int assignmentId = insertAssignment(courseId, instructorId, "Draft");

        doThrow(new IllegalStateException("injected-after-publish"))
                .when(assignmentResponseAssembler)
                .toStaffResponse(any(), any(ZoneId.class), any(), nullable(AssignmentRubricVersion.class),
                        anyInt(), anyInt(), anyInt(), anyInt());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> assignmentService.publish(courseId, assignmentId, instructorId));
        assertEquals("injected-after-publish", thrown.getMessage());

        Assignment assignment = assignmentMapper.selectById(assignmentId);
        assertEquals("Draft", assignment.getState());
        assertEquals(0, assignment.getPublicationVersion());
        assertEquals(0, count("""
                SELECT COUNT(*) FROM assignment_audit_log
                WHERE assignment_id = ? AND action = 'ASSIGNMENT_PUBLISHED'
                """, assignmentId));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_event_outbox
                WHERE notification_type = 'ASSIGNMENT_PUBLISHED' AND subject_id = ?
                """, assignmentId));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM notification_event_recipient r
                JOIN notification_event_outbox o ON o.id = r.outbox_id
                WHERE o.subject_id = ?
                """, assignmentId));
    }

    @Test
    void guardedPublicationSql_rejectsSecondIncrementWithoutUnpublish() {
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        int assignmentId = insertAssignment(courseId, instructorId, "Draft");
        assertEquals(1, assignmentMapper.publishAndIncrementPublicationVersion(assignmentId));
        assertEquals(0, assignmentMapper.publishAndIncrementPublicationVersion(assignmentId));
        int weekId = insertWeek(courseId, "Draft");
        assertEquals(1, courseWeekMapper.publishAndIncrementPublicationVersion(weekId));
        assertEquals(0, courseWeekMapper.publishAndIncrementPublicationVersion(weekId));
        int quizId = insertQuiz(courseId, instructorId, "Draft", 1);
        assertEquals(1, quizMapper.publishAndIncrementPublicationVersion(quizId, 1));
        assertEquals(0, quizMapper.publishAndIncrementPublicationVersion(quizId, 1));
    }
}
