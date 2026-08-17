package com.coursistant.lms.module.interaction.notification.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NotificationPhase2MigrationIT {

    private static MySQLContainer<?> mysql;
    private static JdbcTemplate jdbc;
    private static DataSource dataSource;

    @BeforeAll
    static void start() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required");
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
                .withDatabaseName("lms_notification_p2")
                .withUsername("p2")
                .withPassword("p2")
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
    }

    @Test
    void emptyThenExistingColumns_areIdempotentAndPassGate() throws Exception {
        createVersionlessTables();
        runSql("sql/notification_phase2.sql");
        assertGate();
        runSql("sql/notification_phase2.sql");
        runSql("sql/notification_phase2_gate_check.sql");
        assertGate();
    }

    private void createVersionlessTables() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource("notification-p2-it-schema.sql"));
        }
    }

    private void runSql(String path) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource(path));
        }
    }

    private void assertGate() {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'assignment'
                      AND column_name = 'publication_version'
                      AND is_nullable = 'NO' AND column_default = '0') AS assignment_publication_version,
                  (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'assignment'
                      AND column_name = 'schedule_version'
                      AND is_nullable = 'NO' AND column_default = '0') AS assignment_schedule_version,
                  (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'quiz'
                      AND column_name = 'publication_version'
                      AND is_nullable = 'NO' AND column_default = '0') AS quiz_publication_version,
                  (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'course_week'
                      AND column_name = 'publication_version'
                      AND is_nullable = 'NO' AND column_default = '0') AS course_week_publication_version,
                  (SELECT COUNT(*) FROM assignment
                    WHERE publication_version IS NULL OR publication_version < 0
                       OR schedule_version IS NULL OR schedule_version < 0) AS assignment_invalid_versions,
                  (SELECT COUNT(*) FROM quiz
                    WHERE publication_version IS NULL OR publication_version < 0) AS quiz_invalid_versions,
                  (SELECT COUNT(*) FROM course_week
                    WHERE publication_version IS NULL OR publication_version < 0) AS course_week_invalid_versions
                """);
        assertEquals(1L, ((Number) row.get("assignment_publication_version")).longValue());
        assertEquals(1L, ((Number) row.get("assignment_schedule_version")).longValue());
        assertEquals(1L, ((Number) row.get("quiz_publication_version")).longValue());
        assertEquals(1L, ((Number) row.get("course_week_publication_version")).longValue());
        assertEquals(0L, ((Number) row.get("assignment_invalid_versions")).longValue());
        assertEquals(0L, ((Number) row.get("quiz_invalid_versions")).longValue());
        assertEquals(0L, ((Number) row.get("course_week_invalid_versions")).longValue());
    }
}
