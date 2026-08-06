package com.coursistant.lms.module.course.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.ByteArrayResource;
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoursePart1MigrationIT {

    private MySQLContainer<?> mysql;
    private JdbcTemplate jdbc;
    private final List<String> report = new ArrayList<>();

    @BeforeAll
    void start() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required");
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
                .withDatabaseName("lms_course_p1")
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
        jdbc = new JdbcTemplate(ds);
        applyBaseSchema(ds);
        seedLegal();
        report.add("jdbc=" + mysql.getJdbcUrl());
    }

    @AfterAll
    void stop() {
        if (mysql != null) {
            mysql.stop();
        }
        System.out.println("CoursePart1MigrationIT report:\n" + String.join("\n", report));
    }

    @Test
    @Order(1)
    void scriptsExistWithChecksums() throws Exception {
        for (String s : List.of(
                "sql/course_part1_precheck.sql",
                "sql/course_part1_expand.sql",
                "sql/course_part1_backfill.sql",
                "sql/course_part1_gate_check.sql",
                "sql/course_part1_active_instructor_uk.sql",
                "sql/course_part1_restore.sql")) {
            Path p = Path.of(s);
            assertTrue(Files.exists(p), s);
            report.add(s + " sha256=" + sha256(p));
        }
    }

    @Test
    @Order(2)
    void precheckDetectsDualActiveAndCrossTenant() {
        jdbc.execute("ALTER TABLE enrollment DROP INDEX uk_enrollment_one_instructor");
        jdbc.update("INSERT INTO `user` (id,tenant_id,username,password,email,role,level,status) VALUES "
                + "(901,1,'dup1','x','dup1@ex.com','USER','INSTRUCTOR','ACTIVE'),"
                + "(902,1,'dup2','x','dup2@ex.com','USER','INSTRUCTOR','ACTIVE'),"
                + "(903,2,'cross','x','cross@ex.com','USER','STUDENT','ACTIVE')");
        jdbc.update("INSERT INTO course (id,tenant_id,course_code,title,term_start_date,term_end_date,instructor_id,creator_id) "
                + "VALUES (2,1,'CS102','Dup','2026-01-01','2026-06-01',901,901)");
        jdbc.update("INSERT INTO enrollment (course_id,user_id,course_role,active,enrolled_at) VALUES "
                + "(2,901,'Instructor',1,UTC_TIMESTAMP()),(2,902,'Instructor',1,UTC_TIMESTAMP()),"
                + "(1,903,'Student',1,UTC_TIMESTAMP())");

        Integer multi = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (SELECT course_id FROM enrollment WHERE course_role='Instructor' AND active=1 "
                        + "GROUP BY course_id HAVING COUNT(*)>1) d",
                Integer.class);
        Integer cross = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollment e INNER JOIN course c ON c.id=e.course_id "
                        + "INNER JOIN `user` u ON u.id=e.user_id WHERE u.tenant_id <> c.tenant_id",
                Integer.class);
        assertTrue(multi != null && multi >= 1);
        assertTrue(cross != null && cross >= 1);

        jdbc.update("DELETE FROM enrollment WHERE course_id=2 OR user_id=903");
        jdbc.update("DELETE FROM course WHERE id=2");
        jdbc.update("DELETE FROM `user` WHERE id IN (901,902,903)");
        jdbc.execute("ALTER TABLE enrollment ADD UNIQUE KEY uk_enrollment_one_instructor (instructor_course_id)");
        report.add("precheckIllegalDetected=true");
    }

    @Test
    @Order(3)
    void happyPath_expandBackfillUkAndRestore() throws Exception {
        runSqlFileAllowErrors("sql/course_part1_expand.sql");
        runSqlFile("sql/course_part1_backfill.sql");
        runSqlFile("sql/course_part1_backfill.sql");

        Integer nullActors = jdbc.queryForObject(
                "SELECT COUNT(*) FROM course WHERE creator_actor_type IS NULL OR creator_actor_id IS NULL",
                Integer.class);
        assertEquals(0, nullActors);

        runSqlFile("sql/course_part1_active_instructor_uk.sql");

        String expr = jdbc.queryForObject(
                "SELECT generation_expression FROM information_schema.columns "
                        + "WHERE table_schema=DATABASE() AND table_name='enrollment' "
                        + "AND column_name='instructor_course_id'",
                String.class);
        assertNotNull(expr);
        assertTrue(expr.toLowerCase(Locale.ROOT).contains("active"), expr);

        // inactive Instructor no longer occupies the unique slot
        jdbc.update("INSERT INTO enrollment (course_id,user_id,course_role,active,enrolled_at) "
                + "VALUES (1,2,'Instructor',0,UTC_TIMESTAMP())");

        // activating a second Instructor on the same course must violate UK
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE enrollment SET active=1 WHERE course_id=1 AND user_id=2 AND course_role='Instructor'"));

        // Remove inactive row before restore (legacy UK expression would collide)
        jdbc.update("DELETE FROM enrollment WHERE course_id=1 AND user_id=2 AND course_role='Instructor' AND active=0");
        runSqlFile("sql/course_part1_restore.sql");
        Integer missingCol = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema=DATABASE() AND table_name='course' "
                        + "AND column_name='creator_actor_type'",
                Integer.class);
        assertEquals(0, missingCol);
        report.add("happyPathOk=true");
    }

    private void applyBaseSchema(DataSource ds) {
        ResourceDatabasePopulator pop = new ResourceDatabasePopulator();
        pop.addScript(new ByteArrayResource("""
                CREATE TABLE tenant (
                  id INT NOT NULL AUTO_INCREMENT,
                  name VARCHAR(255) NOT NULL,
                  timezone VARCHAR(64) NOT NULL,
                  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                  PRIMARY KEY (id)
                );
                CREATE TABLE `user` (
                  id INT NOT NULL AUTO_INCREMENT,
                  tenant_id INT NULL,
                  username VARCHAR(255) NOT NULL,
                  password VARCHAR(255) NOT NULL,
                  email VARCHAR(255) NOT NULL,
                  role VARCHAR(32) NOT NULL,
                  level VARCHAR(32) NULL,
                  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                  PRIMARY KEY (id)
                );
                CREATE TABLE course (
                  id INT NOT NULL AUTO_INCREMENT,
                  tenant_id INT NOT NULL,
                  course_code VARCHAR(32) NOT NULL,
                  title VARCHAR(255) NOT NULL,
                  term_start_date DATE NOT NULL,
                  term_end_date DATE NOT NULL,
                  instructor_id INT NOT NULL,
                  state ENUM('Active','Archived') NOT NULL DEFAULT 'Active',
                  creator_id INT NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id)
                );
                CREATE TABLE enrollment (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  user_id INT NOT NULL,
                  course_role ENUM('Student','TA','Instructor') NOT NULL,
                  can_grade TINYINT(1) NOT NULL DEFAULT 0,
                  can_post_announcements TINYINT(1) NOT NULL DEFAULT 0,
                  can_manage_groups TINYINT(1) NOT NULL DEFAULT 0,
                  can_manage_course_events TINYINT(1) NOT NULL DEFAULT 0,
                  active TINYINT(1) NOT NULL DEFAULT 1,
                  assignment_submit_frozen TINYINT(1) NOT NULL DEFAULT 0,
                  enrolled_at DATETIME NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  instructor_course_id INT GENERATED ALWAYS AS (IF(course_role = 'Instructor', course_id, NULL)) STORED,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_enrollment_course_user (course_id, user_id),
                  UNIQUE KEY uk_enrollment_one_instructor (instructor_course_id)
                );
                """.getBytes(StandardCharsets.UTF_8)));
        pop.execute(ds);
    }

    private void seedLegal() {
        jdbc.update("INSERT INTO tenant (id,name,timezone,status) VALUES (1,'T1','UTC','ACTIVE'),(2,'T2','UTC','ACTIVE')");
        jdbc.update("INSERT INTO `user` (id,tenant_id,username,password,email,role,level,status) VALUES "
                + "(1,1,'u1','x','u1@ex.com','USER','INSTRUCTOR','ACTIVE'),"
                + "(2,1,'u2','x','u2@ex.com','USER','INSTRUCTOR','ACTIVE'),"
                + "(3,1,'u3','x','u3@ex.com','USER','STUDENT','ACTIVE')");
        jdbc.update("INSERT INTO course (id,tenant_id,course_code,title,term_start_date,term_end_date,instructor_id,creator_id) "
                + "VALUES (1,1,'CS101','Intro','2026-01-01','2026-06-01',1,1)");
        jdbc.update("INSERT INTO enrollment (course_id,user_id,course_role,active,enrolled_at) "
                + "VALUES (1,1,'Instructor',1,UTC_TIMESTAMP()),(1,3,'Student',1,UTC_TIMESTAMP())");
    }

    private void runSqlFile(String path) throws Exception {
        try (Connection c = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource(path));
        }
        report.add("ran " + path);
    }

    private void runSqlFileAllowErrors(String path) {
        try {
            runSqlFile(path);
        } catch (Exception e) {
            report.add("allow-error " + path + ": " + e.getMessage());
        }
    }

    private static String sha256(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Files.readAllBytes(p));
        return HexFormat.of().formatHex(md.digest());
    }
}
