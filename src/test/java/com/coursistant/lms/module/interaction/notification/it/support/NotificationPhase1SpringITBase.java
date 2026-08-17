package com.coursistant.lms.module.interaction.notification.it.support;

import com.coursistant.lms.module.interaction.notification.email.FakeNotificationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.UUID;

@SpringBootTest(
        classes = NotificationPhase1SpringITConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("notification-p1")
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(DockerRequiredCondition.class)
public abstract class NotificationPhase1SpringITBase {

    private static final String MYSQL_IMAGE = "mysql:8.0.36";
    private static final boolean DOCKER = detectDocker();

    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = DOCKER
            ? new MySQLContainer<>(DockerImageName.parse(MYSQL_IMAGE))
            .withDatabaseName("lms_notification_p1_spring")
            .withUsername("p1s")
            .withPassword("p1s")
            .withReuse(true)
            : null;

    static {
        if (DOCKER) {
            MYSQL.start();
        }
    }

    private static boolean detectDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        if (!DOCKER) {
            return;
        }
        registry.add("spring.datasource.url", () -> {
            String base = MYSQL.getJdbcUrl();
            String sep = base.contains("?") ? "&" : "?";
            return base + sep + "allowMultiQueries=true&allowPublicKeyRetrieval=true"
                    + "&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";
        });
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("DB_USERNAME", MYSQL::getUsername);
        registry.add("DB_PASSWORD", MYSQL::getPassword);
        registry.add("MAIL_USERNAME", () -> "test@example.com");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("MINIO_ACCESS_KEY", () -> "test");
        registry.add("MINIO_SECRET_KEY", () -> "test");
        registry.add("API_BASE_URL", () -> "http://localhost");
        registry.add("REDIS_DEFAULT_PASSWORD", () -> "");
        registry.add("aws.s3.enabled", () -> "false");
        registry.add("aws.s3.bucket", () -> "notification-p1-it");
        registry.add("token.private-key-path", () -> "classpath:test-private.pem");
        registry.add("token.public-key-path", () -> "classpath:test-public.pem");
    }

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected FakeNotificationEmailSender fakeNotificationEmailSender;

    private static boolean schemaApplied;

    @BeforeEach
    void resetNotificationState() {
        ensureSchema();
        fakeNotificationEmailSender.reset();
        wipeTables();
        jdbcTemplate.update(
                "INSERT INTO tenant (id, name, timezone, status) VALUES (1, 'p1-tenant', 'America/Los_Angeles', 'ACTIVE')");
    }

    private void ensureSchema() {
        if (schemaApplied) {
            return;
        }
        synchronized (NotificationPhase1SpringITBase.class) {
            if (schemaApplied) {
                return;
            }
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("notification-p1-it-schema.sql"));
            populator.addScript(new FileSystemResource("sql/notification_phase1.sql"));
            populator.setContinueOnError(false);
            populator.setSeparator(";");
            populator.execute(dataSource);
            schemaApplied = true;
        }
    }

    protected void wipeTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        for (String table : new String[]{
                "notification_event_recipient",
                "notification_event_outbox",
                "notification_delivery",
                "notification_digest_email",
                "user_notification",
                "enrollment",
                "course",
                "user",
                "tenant"
        }) {
            jdbcTemplate.update("DELETE FROM `" + table + "`");
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    protected int insertUser(String email, boolean emailNotifications, String status) {
        String username = "u-" + UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO `user` (tenant_id, username, password, name, email, role, level, status,
                          email_notifications)
                        VALUES (1, ?, 'x', 'Test', ?, 'USER', 'STUDENT', ?, ?)
                        """,
                username, email, status, emailNotifications ? 1 : 0);
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM `user` WHERE email = ?", Integer.class, email);
        return id == null ? -1 : id;
    }

    protected int insertInstructor() {
        String email = "instr-" + UUID.randomUUID() + "@example.com";
        String username = "i-" + UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO `user` (tenant_id, username, password, name, email, role, level, status,
                          email_notifications)
                        VALUES (1, ?, 'x', 'Instructor', ?, 'USER', 'INSTRUCTOR', 'ACTIVE', 1)
                        """,
                username, email);
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM `user` WHERE email = ?", Integer.class, email);
        return id == null ? -1 : id;
    }

    protected int insertCourse(int instructorId) {
        jdbcTemplate.update("""
                        INSERT INTO course (tenant_id, course_code, title, term_start_date, term_end_date,
                          instructor_id, state, creator_id)
                        VALUES (1, 'P1', 'Phase 1', '2026-01-01', '2026-12-31', ?, 'Active', ?)
                        """,
                instructorId, instructorId);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM course", Integer.class);
        return id == null ? -1 : id;
    }

    protected void enrollStudent(int courseId, int userId) {
        jdbcTemplate.update("""
                        INSERT INTO enrollment (course_id, user_id, course_role, active, enrolled_at)
                        VALUES (?, ?, 'Student', 1, UTC_TIMESTAMP())
                        """,
                courseId, userId);
    }

    protected void archiveCourse(int courseId) {
        jdbcTemplate.update("""
                        UPDATE course
                        SET archived_at = UTC_TIMESTAMP(), state = 'Archived'
                        WHERE id = ?
                        """,
                courseId);
    }

    protected long insertOutbox(String eventId, String type, String subjectType, int subjectId, String eventKey,
                                String recipientMode, String status, int courseId) {
        jdbcTemplate.update("""
                        INSERT INTO notification_event_outbox (
                          event_id, tenant_id, course_id, notification_type, subject_type, subject_id, event_key,
                          message, deep_link, occurred_at, recipient_mode, status, attempt_count, next_attempt_at,
                          created_at, updated_at
                        ) VALUES (?, 1, ?, ?, ?, ?, ?, 'msg', '/x', UTC_TIMESTAMP(3), ?, ?, 0,
                          DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                        """,
                eventId, courseId, type, subjectType, subjectId, eventKey, recipientMode, status);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM notification_event_outbox WHERE event_id = ?", Long.class, eventId);
        return id == null ? -1L : id;
    }

    protected long insertDelivery(String eventId, int recipientUserId, String type, String subjectType,
                                  int subjectId, String eventKey, String channel, String status,
                                  int courseId, java.time.LocalDate digestDate) {
        jdbcTemplate.update("""
                        INSERT INTO notification_delivery (
                          event_id, tenant_id, recipient_user_id, course_id, notification_type, subject_type,
                          subject_id, event_key, channel, status, message, deep_link, occurred_at, digest_date,
                          attempt_count, next_attempt_at, unknown_outcome_count, created_at, updated_at
                        ) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, 'msg', '/x', UTC_TIMESTAMP(3), ?,
                          0, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE), 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                        """,
                eventId, recipientUserId, courseId, type, subjectType, subjectId, eventKey, channel, status, digestDate);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM notification_delivery WHERE event_id = ? AND recipient_user_id = ? AND channel = ?",
                Long.class, eventId, recipientUserId, channel);
        return id == null ? -1L : id;
    }

    protected String uuid() {
        return UUID.randomUUID().toString();
    }

    protected int count(String sql, Object... args) {
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
