package com.coursistant.lms.module.interaction.notification.it;

import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class NotificationPhase1Mysql {

    private static final Object LOCK = new Object();
    private static MySQLContainer<?> mysql;
    private static JdbcTemplate jdbc;
    private static DataSource dataSource;

    private NotificationPhase1Mysql() {
    }

    static JdbcTemplate jdbc() {
        ensureStarted();
        return jdbc;
    }

    static DataSource dataSource() {
        ensureStarted();
        return dataSource;
    }

    static void ensureStarted() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required");
        synchronized (LOCK) {
            if (jdbc != null) {
                return;
            }
            mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
                    .withDatabaseName("lms_notification_p1")
                    .withUsername("p1")
                    .withPassword("p1")
                    .withReuse(false);
            mysql.start();
            DriverManagerDataSource ds = new DriverManagerDataSource();
            String base = mysql.getJdbcUrl();
            String sep = base.contains("?") ? "&" : "?";
            ds.setUrl(base + sep + "allowMultiQueries=true&allowPublicKeyRetrieval=true");
            ds.setUsername(mysql.getUsername());
            ds.setPassword(mysql.getPassword());
            dataSource = ds;
            jdbc = new JdbcTemplate(ds);
            runSql("sql/notification_phase1.sql");
        }
    }

    static void runSql(String path) {
        try (Connection c = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource(path));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run " + path, e);
        }
    }

    static String uuid() {
        return UUID.randomUUID().toString();
    }

    static long insertDelivery(String eventId, int recipientUserId, String type, String subjectType,
                               int subjectId, String eventKey, String channel, String status) {
        jdbc.update("""
                INSERT INTO notification_delivery (
                  event_id, tenant_id, recipient_user_id, course_id, notification_type, subject_type,
                  subject_id, event_key, channel, status, message, deep_link, occurred_at,
                  attempt_count, next_attempt_at, unknown_outcome_count, created_at, updated_at
                ) VALUES (?, 1, ?, 2, ?, ?, ?, ?, ?, ?, 'msg', '/x', UTC_TIMESTAMP(3),
                  0, UTC_TIMESTAMP(3), 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, eventId, recipientUserId, type, subjectType, subjectId, eventKey, channel, status);
        Long id = jdbc.queryForObject(
                "SELECT id FROM notification_delivery WHERE event_id = ? AND recipient_user_id = ? AND channel = ?",
                Long.class, eventId, recipientUserId, channel);
        return id == null ? -1L : id;
    }

    static long insertOutbox(String eventId, String type, String subjectType, int subjectId, String eventKey,
                             String status) {
        jdbc.update("""
                INSERT INTO notification_event_outbox (
                  event_id, tenant_id, course_id, notification_type, subject_type, subject_id, event_key,
                  message, deep_link, occurred_at, recipient_mode, status, attempt_count, next_attempt_at,
                  created_at, updated_at
                ) VALUES (?, 1, 2, ?, ?, ?, ?, 'msg', '/x', UTC_TIMESTAMP(3), 'EXPLICIT', ?, 0,
                  UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, eventId, type, subjectType, subjectId, eventKey, status);
        Long id = jdbc.queryForObject(
                "SELECT id FROM notification_event_outbox WHERE event_id = ?", Long.class, eventId);
        return id == null ? -1L : id;
    }
}
