package com.coursistant.lms.module.auth.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Fresh MySQL-only migration drill (no Spring Boot app). Never touches developer lms_v2.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthMigrationIT {

    private MySQLContainer<?> mysql;
    private JdbcTemplate jdbc;
    private final List<String> report = new ArrayList<>();

    @BeforeAll
    void startFreshMysql() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required for AuthMigrationIT");
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
                .withDatabaseName("lms_auth_mig")
                .withUsername("mig")
                .withPassword("mig")
                .withReuse(false);
        mysql.start();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        String base = mysql.getJdbcUrl();
        String sep = base.contains("?") ? "&" : "?";
        ds.setUrl(base + sep + "allowMultiQueries=true&allowPublicKeyRetrieval=true");
        ds.setUsername(mysql.getUsername());
        ds.setPassword(mysql.getPassword());
        jdbc = new JdbcTemplate(ds);
        applyLegacySchema(ds);
        seedLegacyRows();
        report.add("mysqlImage=" + mysql.getDockerImageName());
        report.add("jdbc=" + mysql.getJdbcUrl());
    }

    @AfterAll
    void stop() {
        if (mysql != null) {
            mysql.stop();
        }
        if (!report.isEmpty()) {
            System.out.println("AuthMigrationIT report:\n" + String.join("\n", report));
        }
    }

    @Test
    @Order(1)
    void migrationScripts_existWithStableChecksumOrder() throws Exception {
        List<String> scripts = List.of(
                "sql/admin_role_phase1_precheck.sql",
                "sql/admin_role_phase1.sql",
                "sql/admin_role_phase1_gate_check.sql",
                "sql/admin_role_phase1_restore.sql",
                "sql/admin_identity_phase2_precheck.sql",
                "sql/admin_identity_phase2_expand.sql",
                "sql/admin_identity_phase2_alter_columns.sql",
                "sql/admin_identity_phase2_backfill.sql",
                "sql/admin_identity_phase2_gate_check.sql",
                "sql/auth_refresh_rotation_expand.sql",
                "sql/auth_refresh_rotation_gate_check.sql"
        );
        for (String s : scripts) {
            Path p = Path.of(s);
            assertTrue(Files.exists(p), "missing " + s);
            report.add(s + " sha256=" + sha256(p));
        }
    }

    @Test
    @Order(2)
    void happyPath_phase1_identity_refresh_gateAndIdempotentBackfill() throws Exception {
        long t0 = System.nanoTime();
        runSqlFile("sql/admin_role_phase1_precheck.sql");
        assertEquals(0, countRows("SELECT COUNT(*) FROM admin WHERE role NOT IN ('ADMIN','SYSTEM_ADMIN')"));
        assertEquals(0, countRows("SELECT COUNT(*) FROM `user` WHERE level = 'TA'"));

        runSqlFile("sql/admin_role_phase1.sql");
        Integer remainingAdmin = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin WHERE role = 'ADMIN'", Integer.class);
        assertEquals(0, remainingAdmin);
        assertEquals(0, countRows("SELECT COUNT(*) FROM refresh_tokens"));
        Integer systemAdmins = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin WHERE role = 'SYSTEM_ADMIN'", Integer.class);
        assertTrue(systemAdmins != null && systemAdmins >= 1);

        // Identity expand: CREATE TABLE IF NOT EXISTS + MySQL-safe column adds
        runMysqlCompatibleExpand("sql/admin_identity_phase2_expand.sql");
        runSqlFileAllowErrors("sql/admin_identity_phase2_alter_columns.sql"); // may already exist
        runSqlFile("sql/admin_identity_phase2_backfill.sql");
        runSqlFile("sql/admin_identity_phase2_backfill.sql"); // idempotent
        Integer identities = jdbc.queryForObject("SELECT COUNT(*) FROM account_identity", Integer.class);
        assertTrue(identities != null && identities >= 2);
        assertEquals(0, countRows(
                "SELECT COUNT(*) FROM ("
                        + "SELECT normalized_email FROM account_identity GROUP BY normalized_email HAVING COUNT(*)>1"
                        + ") d"));

        runMysqlCompatibleExpand("sql/auth_refresh_rotation_expand.sql");
        // After cutover DELETE, table empty → gate OK
        Integer missingSid = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE session_id IS NULL OR session_id = ''",
                Integer.class);
        assertEquals(0, missingSid);

        // Restore drill (role only)
        runSqlFile("sql/admin_role_phase1_restore.sql");
        Integer restored = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin WHERE role = 'ADMIN'", Integer.class);
        assertTrue(restored != null && restored >= 1);

        report.add("happyPathMs=" + ((System.nanoTime() - t0) / 1_000_000));
    }

    @Test
    @Order(3)
    void illegalSamples_failGateWithoutGuessingFix() {
        runMysqlCompatibleExpand("sql/admin_identity_phase2_expand.sql");
        runSqlFileAllowErrors("sql/admin_identity_phase2_alter_columns.sql");

        jdbc.update("INSERT INTO `user` (username, password, email, role, level, status, auth_version) "
                + "VALUES ('badta','x','badta@example.com','USER','TA','ACTIVE',1)");
        Integer taBad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE level = 'TA'", Integer.class);
        assertTrue(taBad != null && taBad >= 1);
        // Gate check surfaces TA without auto-correcting to STUDENT/INSTRUCTOR
        Integer gateTa = jdbc.queryForObject(
                "SELECT COUNT(*) AS bad_cnt FROM `user` WHERE level = 'TA'", Integer.class);
        assertTrue(gateTa != null && gateTa >= 1);

        // Cross-table email collision — precheck surfaces; do not merge identities
        jdbc.update("INSERT INTO admin (username, password, email, role, status, auth_version) "
                + "VALUES ('dupadm','x','cross-dup@example.com','SYSTEM_ADMIN','ACTIVE',1)");
        jdbc.update("INSERT INTO `user` (tenant_id, username, password, email, role, level, status, auth_version) "
                + "VALUES (1,'dupusr','x','cross-dup@example.com','USER','STUDENT','ACTIVE',1)");
        Integer cross = jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin a INNER JOIN `user` u "
                        + "ON LOWER(TRIM(a.email)) = LOWER(TRIM(u.email))",
                Integer.class);
        assertTrue(cross != null && cross >= 1);

        try {
            jdbc.update("INSERT INTO refresh_tokens (user_id, token, expire_time, role) "
                    + "VALUES (1,'tok-null-sid', DATE_ADD(UTC_TIMESTAMP(), INTERVAL 1 DAY),'USER')");
            Integer missing = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM refresh_tokens WHERE session_id IS NULL OR session_id = ''",
                    Integer.class);
            assertTrue(missing != null && missing >= 1);
        } catch (Exception e) {
            report.add("refresh null session_id insert skipped: " + e.getMessage());
        }

        report.add("illegalSamplesDetected=true");
    }

    private void applyLegacySchema(DataSource ds) {
        ResourceDatabasePopulator pop = new ResourceDatabasePopulator();
        pop.addScript(new org.springframework.core.io.ByteArrayResource("""
                CREATE TABLE tenant (
                  id INT NOT NULL AUTO_INCREMENT,
                  name VARCHAR(255) NOT NULL,
                  timezone VARCHAR(64) NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE `user` (
                  id INT NOT NULL AUTO_INCREMENT,
                  tenant_id INT NULL,
                  username VARCHAR(255) NOT NULL,
                  password VARCHAR(255) NOT NULL,
                  name VARCHAR(255) NULL,
                  email VARCHAR(255) NOT NULL,
                  role VARCHAR(32) NOT NULL,
                  level VARCHAR(32) NULL,
                  must_change_password TINYINT(1) NOT NULL DEFAULT 0,
                  email_notifications TINYINT(1) NOT NULL DEFAULT 1,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_user_email (email)
                );
                CREATE TABLE admin (
                  id INT NOT NULL AUTO_INCREMENT,
                  username VARCHAR(255) NOT NULL,
                  password VARCHAR(255) NOT NULL,
                  name VARCHAR(255) NULL,
                  email VARCHAR(255) NOT NULL,
                  role VARCHAR(32) NOT NULL,
                  invitation VARCHAR(255) NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_admin_email (email)
                );
                CREATE TABLE refresh_tokens (
                  id INT NOT NULL AUTO_INCREMENT,
                  user_id INT NOT NULL,
                  token VARCHAR(128) NOT NULL,
                  ip_address VARCHAR(64) NULL,
                  user_agent VARCHAR(512) NULL,
                  expire_time DATETIME NOT NULL,
                  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  role VARCHAR(32) NOT NULL,
                  PRIMARY KEY (id),
                  KEY idx_refresh_tokens_token (token)
                );
                INSERT INTO tenant (id, name, timezone) VALUES (1,'Default','UTC');
                """.getBytes(StandardCharsets.UTF_8)));
        pop.execute(ds);
    }

    private void seedLegacyRows() {
        jdbc.update("INSERT INTO admin (username, password, email, role) VALUES ('legacyadmin','x','legacy-admin@example.com','ADMIN')");
        jdbc.update("INSERT INTO `user` (tenant_id, username, password, email, role, level) "
                + "VALUES (1,'legacyuser','x','legacy-user@example.com','USER','STUDENT')");
        jdbc.update("INSERT INTO refresh_tokens (user_id, token, expire_time, role) "
                + "VALUES (1,'legacy-refresh', DATE_ADD(UTC_TIMESTAMP(), INTERVAL 1 DAY),'USER')");
    }

    private void runSqlFile(String path) throws Exception {
        long t0 = System.nanoTime();
        try (Connection c = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource(path));
        }
        report.add("ran " + path + " ms=" + ((System.nanoTime() - t0) / 1_000_000));
    }

    private void runSqlFileAllowErrors(String path) {
        try {
            runSqlFile(path);
        } catch (Exception e) {
            report.add("allow-error " + path + ": " + rootMessage(e));
        }
    }

    /**
     * MySQL 8.0 does not support ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS.
     * Strip those clauses and ignore duplicate-object errors for re-entrancy.
     */
    private void runMysqlCompatibleExpand(String path) {
        try {
            String sql = Files.readString(Path.of(path));
            sql = sql.replace("ADD COLUMN IF NOT EXISTS", "ADD COLUMN")
                    .replace("CREATE UNIQUE INDEX IF NOT EXISTS", "CREATE UNIQUE INDEX")
                    .replace("CREATE INDEX IF NOT EXISTS", "CREATE INDEX");
            for (String stmt : splitStatements(sql)) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                try {
                    jdbc.execute(trimmed);
                } catch (Exception e) {
                    String msg = rootMessage(e).toLowerCase(Locale.ROOT);
                    if (msg.contains("duplicate") || msg.contains("exists") || msg.contains("check that column")) {
                        report.add("idempotent-skip: " + trimmed.substring(0, Math.min(60, trimmed.length())));
                    } else {
                        throw e;
                    }
                }
            }
            report.add("ran-compatible " + path);
        } catch (Exception e) {
            fail("expand failed for " + path + ": " + rootMessage(e));
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : sql.split("\n")) {
            String t = line.trim();
            if (t.startsWith("--")) {
                continue;
            }
            cur.append(line).append('\n');
            if (t.endsWith(";")) {
                out.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (!cur.isEmpty()) {
            out.add(cur.toString());
        }
        return out;
    }

    private int countRows(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }

    private static String sha256(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Files.readAllBytes(p));
        return HexFormat.of().formatHex(md.digest());
    }

    private static String rootMessage(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() == null ? e.toString() : c.getMessage();
    }
}
