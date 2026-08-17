package com.coursistant.lms.module.interaction.notification.it.support;

import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

@SpringBootTest(
        classes = {NotificationPhase1SpringITConfig.class, NotificationPhase2ExtraConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public abstract class NotificationPhase2SpringITBase extends NotificationPhase1SpringITBase {

    private static boolean p2SchemaApplied;

    @Autowired
    protected NotificationPublisher notificationPublisher;

    @Override
    protected void wipeTables() {
        ensurePhase2Schema();
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        for (String table : new String[]{
                "group_membership",
                "course_group",
                "group_set",
                "course_event",
                "course_announcement",
                "course_week",
                "quiz",
                "assignment_submission",
                "assignment"
        }) {
            jdbcTemplate.update("DELETE FROM `" + table + "`");
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        super.wipeTables();
    }

    private void ensurePhase2Schema() {
        if (p2SchemaApplied) {
            return;
        }
        synchronized (NotificationPhase2SpringITBase.class) {
            if (p2SchemaApplied) {
                return;
            }
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("notification-p2-it-schema.sql"));
            populator.addScript(new FileSystemResource("sql/notification_phase2.sql"));
            populator.setContinueOnError(false);
            populator.setSeparator(";");
            populator.execute(dataSource);
            p2SchemaApplied = true;
        }
    }

    protected void enrollRole(int courseId, int userId, String role) {
        jdbcTemplate.update("""
                        INSERT INTO enrollment (course_id, user_id, course_role, active, enrolled_at)
                        VALUES (?, ?, ?, 1, UTC_TIMESTAMP())
                        """,
                courseId, userId, role);
    }

    protected void deactivateEnrollment(int courseId, int userId) {
        jdbcTemplate.update("""
                        UPDATE enrollment
                        SET active = 0, withdrawn_at = UTC_TIMESTAMP()
                        WHERE course_id = ? AND user_id = ?
                        """,
                courseId, userId);
    }

    protected int insertAssignment(int courseId, int createdBy, String state) {
        jdbcTemplate.update("""
                        INSERT INTO assignment (course_id, title, description, points_possible, due_at,
                          submission_type, allowed_file_types, max_file_size_bytes, max_file_count, state, created_by)
                        VALUES (?, 'HW', 'desc', 10, '2026-09-01 12:00:00', 'Individual', CAST('[]' AS JSON),
                          1024, 1, ?, ?)
                        """,
                courseId, state, createdBy);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM assignment", Integer.class);
        return id == null ? -1 : id;
    }

    protected int insertQuiz(int courseId, int createdBy, String state, int version) {
        jdbcTemplate.update("""
                        INSERT INTO quiz (course_id, title, instructions, opens_at, closes_at, time_limit_seconds,
                          attempts_allowed, result_visibility, state, version, created_by, created_at, updated_at)
                        VALUES (?, 'Midterm', NULL, '2026-09-01 10:00:00', '2026-09-01 12:00:00', 3600,
                          1, 'AfterClose', ?, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                        """,
                courseId, state, version, createdBy);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM quiz", Integer.class);
        return id == null ? -1 : id;
    }

    protected int insertWeek(int courseId, String state) {
        jdbcTemplate.update("""
                        INSERT INTO course_week (course_id, title, order_position, state)
                        VALUES (?, 'Week 1', 0, ?)
                        """,
                courseId, state);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM course_week", Integer.class);
        return id == null ? -1 : id;
    }

    protected Long publishExplicit(int courseId, NotificationType type, SubjectType subjectType, int subjectId,
                                   String eventKey, Integer actorUserId, List<Integer> recipients) {
        NotificationDispatchPayload payload = new NotificationDispatchPayload();
        payload.setTenantId(1);
        payload.setCourseId(courseId);
        payload.setNotificationType(type);
        payload.setSubjectType(subjectType);
        payload.setSubjectId(subjectId);
        payload.setEventKey(eventKey);
        payload.setMessage(type.name());
        payload.setDeepLink("/courses/" + courseId + "/x/" + subjectId);
        payload.setActorUserId(actorUserId);
        payload.setRecipientMode(RecipientMode.EXPLICIT);
        payload.setRecipientIds(recipients);
        return transactionTemplate.execute(status -> notificationPublisher.publishInTransaction(payload));
    }

    protected String outboxRecipientMode(long outboxId) {
        return jdbcTemplate.queryForObject(
                "SELECT recipient_mode FROM notification_event_outbox WHERE id = ?", String.class, outboxId);
    }

    protected int countInApp(String type, int courseId) {
        return count("""
                SELECT COUNT(*) FROM user_notification
                WHERE notification_type = ? AND course_id = ?
                """, type, courseId);
    }

    protected int countDelivery(String type, String channel) {
        return count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE notification_type = ? AND channel = ?
                """, type, channel);
    }
}
