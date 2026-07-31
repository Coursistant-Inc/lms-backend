package com.coursistant.lms.module.interaction.notification;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Live migration smoke against an isolated {@code *_test} database via {@code mysql} CLI.
 * Never targets {@code lms_v2}. Skip when env or mysql CLI is absent; release still requires a real run.
 */
class NotificationMigrationSmokeTest {

    private static final String JDBC_URL_ENV = "NOTIFICATION_MIGRATION_TEST_JDBC_URL";
    private static final String USER_ENV = "NOTIFICATION_MIGRATION_TEST_USERNAME";
    private static final String PASSWORD_ENV = "NOTIFICATION_MIGRATION_TEST_PASSWORD";

    @Test
    void rejectsNonTestDatabaseName() {
        String url = System.getenv(JDBC_URL_ENV);
        Assumptions.assumeTrue(url != null && !url.isBlank(), "migration test JDBC URL not set");
        String dbName = databaseName(url);
        if (!dbName.endsWith("_test")) {
            fail("Refusing migration smoke: database name must end with _test, got: " + dbName);
        }
        assertTrue(dbName.endsWith("_test"));
    }

    @Test
    void fullMigration_thenNewFormatInsert_andPartialDrop() throws Exception {
        String url = System.getenv(JDBC_URL_ENV);
        String user = System.getenv(USER_ENV);
        String password = System.getenv(PASSWORD_ENV);
        Assumptions.assumeTrue(url != null && !url.isBlank(), "migration test JDBC URL not set");
        Assumptions.assumeTrue(user != null && !user.isBlank(), "migration test username not set");
        Assumptions.assumeTrue(password != null, "migration test password not set");

        String dbName = databaseName(url);
        if ("lms_v2".equalsIgnoreCase(dbName) || !dbName.endsWith("_test")) {
            fail("Refusing migration smoke on unsafe database: " + dbName);
        }

        String mysqlBin = findMysqlCli();
        Assumptions.assumeTrue(mysqlBin != null, "mysql CLI not available");

        Path fixture = Files.createTempFile("notif-legacy-", ".sql");
        Path partialFixture = Files.createTempFile("notif-partial-", ".sql");
        try {
            Files.writeString(fixture, legacyTableFixtureSql(true), StandardCharsets.UTF_8);
            Files.writeString(partialFixture, legacyTableFixtureSql(false), StandardCharsets.UTF_8);

            runMysql(mysqlBin, url, user, password, fixture);
            runMysql(mysqlBin, url, user, password, Path.of("sql/notification_v1.sql"));

            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement st = conn.createStatement()) {
                st.executeUpdate("""
                        INSERT INTO user_notification (
                          tenant_id, recipient_user_id, course_id, notification_type, message,
                          subject_type, subject_id, event_key, deep_link, created_at, read_at
                        ) VALUES (
                          1, 10, 1, 'ANNOUNCEMENT_POSTED', 'hello',
                          'ANNOUNCEMENT', 1, 'smoke:full', '/x', UTC_TIMESTAMP(), NULL
                        )
                        """);
                assertEquals(0, countLegacyColumns(st));
            }

            runMysql(mysqlBin, url, user, password, Path.of("sql/notification_v1_gate_check.sql"));

            // Partial leftover columns: recreate table with only title + new columns mid-state,
            // then follow-up DROP script must clear remaining legacy columns.
            runMysql(mysqlBin, url, user, password, partialFixture);
            runMysql(mysqlBin, url, user, password, Path.of("sql/notification_v1_drop_legacy_columns.sql"));
            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement st = conn.createStatement()) {
                assertEquals(0, countLegacyColumns(st));
            }
        } finally {
            Files.deleteIfExists(fixture);
            Files.deleteIfExists(partialFixture);
        }
    }

    private static int countLegacyColumns(Statement st) throws Exception {
        try (ResultSet rs = st.executeQuery("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'user_notification'
                  AND column_name IN ('event_type', 'ref_id', 'title')
                """)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    /**
     * @param fullLegacy when true, create pre-migration shape with all three legacy columns;
     *                   when false, create a partial leftover with only {@code title}.
     */
    private static String legacyTableFixtureSql(boolean fullLegacy) {
        if (fullLegacy) {
            return """
                    CREATE TABLE IF NOT EXISTS course (
                      id INT NOT NULL PRIMARY KEY,
                      tenant_id INT NOT NULL
                    );
                    INSERT INTO course (id, tenant_id) VALUES (1, 1)
                    ON DUPLICATE KEY UPDATE tenant_id = VALUES(tenant_id);
                    DROP TABLE IF EXISTS user_notification;
                    CREATE TABLE user_notification (
                      id INT NOT NULL AUTO_INCREMENT,
                      recipient_user_id INT NOT NULL,
                      course_id INT NOT NULL,
                      event_type VARCHAR(64) NOT NULL,
                      ref_id INT NOT NULL,
                      title VARCHAR(255) NOT NULL,
                      deep_link VARCHAR(512) NOT NULL,
                      created_at DATETIME NOT NULL,
                      read_at DATETIME NULL,
                      PRIMARY KEY (id),
                      UNIQUE KEY uk_recipient_event_ref (recipient_user_id, event_type, ref_id),
                      KEY idx_user_notification_recipient_created (recipient_user_id, created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    INSERT INTO user_notification
                      (recipient_user_id, course_id, event_type, ref_id, title, deep_link, created_at)
                    VALUES (10, 1, 'ANNOUNCEMENT_POSTED', 1, 'Hello', '/a', UTC_TIMESTAMP());
                    """;
        }
        // Partial leftover: V1 new columns present, only title remains (event_type/ref_id already dropped).
        return """
                DROP TABLE IF EXISTS user_notification;
                CREATE TABLE user_notification (
                  id INT NOT NULL AUTO_INCREMENT,
                  tenant_id INT NOT NULL,
                  recipient_user_id INT NOT NULL,
                  course_id INT NOT NULL,
                  notification_type VARCHAR(64) NOT NULL,
                  message VARCHAR(512) NOT NULL,
                  subject_type VARCHAR(64) NOT NULL,
                  subject_id INT NOT NULL,
                  event_key VARCHAR(128) NOT NULL,
                  title VARCHAR(255) NOT NULL,
                  deep_link VARCHAR(512) NOT NULL,
                  created_at DATETIME NOT NULL,
                  read_at DATETIME NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_notification_dedupe (
                    tenant_id, recipient_user_id, notification_type, subject_type, subject_id, event_key
                  )
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
    }

    private static void runMysql(String mysqlBin, String jdbcUrl, String user, String password, Path sqlFile)
            throws Exception {
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String host = uri.getHost() == null ? "localhost" : uri.getHost();
        int port = uri.getPort() > 0 ? uri.getPort() : 3306;
        String db = databaseName(jdbcUrl);

        ProcessBuilder pb = new ProcessBuilder(
                mysqlBin,
                "-h", host,
                "-P", String.valueOf(port),
                "-u", user,
                "-p" + password,
                db,
                "-e", "source " + sqlFile.toAbsolutePath().toString().replace('\\', '/')
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            fail("mysql timed out running " + sqlFile + "\n" + output);
        }
        if (process.exitValue() != 0) {
            fail("mysql failed (" + process.exitValue() + ") for " + sqlFile + "\n" + output);
        }
    }

    private static String findMysqlCli() {
        String[] candidates = {
                "mysql",
                "mysql.exe",
                "C:\\\\Program Files\\\\MySQL\\\\MySQL Server 5.7\\\\bin\\\\mysql.exe",
                "C:\\\\Program Files\\\\MySQL\\\\MySQL Server 8.0\\\\bin\\\\mysql.exe",
                "C:\\\\xampp\\\\mysql\\\\bin\\\\mysql.exe"
        };
        for (String candidate : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                boolean ok = p.waitFor(5, TimeUnit.SECONDS);
                if (ok && p.exitValue() == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    static String databaseName(String jdbcUrl) {
        String withoutParams = jdbcUrl;
        int q = withoutParams.indexOf('?');
        if (q >= 0) {
            withoutParams = withoutParams.substring(0, q);
        }
        int slash = withoutParams.lastIndexOf('/');
        if (slash < 0 || slash == withoutParams.length() - 1) {
            throw new IllegalArgumentException("Cannot parse database name from JDBC URL");
        }
        return withoutParams.substring(slash + 1);
    }
}
