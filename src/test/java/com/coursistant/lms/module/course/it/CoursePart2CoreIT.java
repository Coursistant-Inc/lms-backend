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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Part 2 DB semantics: empty delete, archive actor, reassignment uniqueness, concurrent reassign.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoursePart2CoreIT {

    private MySQLContainer<?> mysql;
    private JdbcTemplate jdbc;
    private final List<String> report = new ArrayList<>();

    @BeforeAll
    void start() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required");
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
                .withDatabaseName("lms_course_p2")
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
        jdbc = new JdbcTemplate(ds);
        applySchema(ds);
        runSqlFile("sql/course_part1_expand.sql");
        runSqlFileAllowErrors("sql/course_part1_active_instructor_uk.sql");
        runSqlFile("sql/course_part2_expand.sql");
        seed();
        report.add("jdbc=" + mysql.getJdbcUrl());
    }

    @AfterAll
    void stop() {
        if (mysql != null) {
            mysql.stop();
        }
        System.out.println("CoursePart2CoreIT report:\n" + String.join("\n", report));
    }

    @Test
    @Order(1)
    void scriptsExist() {
        assertTrue(Files.exists(Path.of("sql/course_part2_expand.sql")));
        report.add("scriptsOk");
    }

    @Test
    @Order(2)
    void emptyCourseDelete_allowsInstructorOnly() {
        Integer courseId = insertEmptyCourse(50, 1);
        assertTrue(isEmpty(courseId));
        jdbc.update("DELETE FROM enrollment WHERE course_id=? AND course_role='Instructor' AND active=1", courseId);
        jdbc.update("DELETE FROM course WHERE id=?", courseId);
        Integer left = jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE id=?", Integer.class, courseId);
        assertEquals(0, left);
        report.add("emptyDeleteOk");
    }

    @Test
    @Order(3)
    void nonEmpty_withStudent_notEmpty() {
        Integer courseId = insertEmptyCourse(51, 1);
        jdbc.update("INSERT INTO enrollment (course_id,user_id,course_role,active,enrolled_at) VALUES (?,?, 'Student',1,UTC_TIMESTAMP())",
                courseId, 3);
        assertFalse(isEmpty(courseId));
        report.add("nonEmptyDetected");
    }

    @Test
    @Order(4)
    void archiveWritesActorColumns() {
        Integer courseId = insertEmptyCourse(52, 1);
        jdbc.update("UPDATE course SET state='Archived', archived_at=UTC_TIMESTAMP(), "
                        + "archived_by_actor_type='USER', archived_by_actor_id=1 WHERE id=?",
                courseId);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT state, archived_by_actor_type, archived_by_actor_id FROM course WHERE id=?", courseId);
        assertEquals("Archived", row.get("state"));
        assertEquals("USER", row.get("archived_by_actor_type"));
        assertEquals(1, ((Number) row.get("archived_by_actor_id")).intValue());
        report.add("archiveActorOk");
    }

    @Test
    @Order(5)
    void reassignment_uniqueActiveInstructorEnforced() {
        Integer courseId = insertEmptyCourse(53, 1);
        // deactivate current
        jdbc.update("UPDATE enrollment SET active=0 WHERE course_id=? AND course_role='Instructor'", courseId);
        jdbc.update("INSERT INTO enrollment (course_id,user_id,course_role,active,enrolled_at) VALUES (?,?, 'Instructor',1,UTC_TIMESTAMP())",
                courseId, 2);
        Integer active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollment WHERE course_id=? AND course_role='Instructor' AND active=1",
                Integer.class, courseId);
        assertEquals(1, active);
        assertThrows(Exception.class, () -> jdbc.update(
                "INSERT INTO enrollment (course_id,user_id,course_role,active,enrolled_at) VALUES (?,?, 'Instructor',1,UTC_TIMESTAMP())",
                courseId, 1));
        report.add("reassignUkOk");
    }

    @Test
    @Order(6)
    void concurrentReassign_onlyOneActiveInstructor() throws Exception {
        Integer courseId = insertEmptyCourse(54, 1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        Future<?> f1 = pool.submit(() -> tryPromote(courseId, 2, start, success, fail));
        Future<?> f2 = pool.submit(() -> tryPromote(courseId, 4, start, success, fail));
        start.countDown();
        f1.get(20, TimeUnit.SECONDS);
        f2.get(20, TimeUnit.SECONDS);
        pool.shutdown();

        Integer active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollment WHERE course_id=? AND course_role='Instructor' AND active=1",
                Integer.class, courseId);
        assertEquals(1, active);
        assertTrue(success.get() + fail.get() >= 1);
        report.add("concurrentReassign active=" + active + " success=" + success + " fail=" + fail);
    }

    private void tryPromote(Integer courseId, int newInstructorId, CountDownLatch start,
                            AtomicInteger success, AtomicInteger fail) {
        try {
            start.await(10, TimeUnit.SECONDS);
            jdbc.execute((Connection conn) -> {
                conn.setAutoCommit(false);
                try {
                    // lock course + current instructor
                    conn.prepareStatement("SELECT id FROM course WHERE id=" + courseId + " FOR UPDATE").executeQuery().close();
                    var rs = conn.prepareStatement(
                            "SELECT id,user_id FROM enrollment WHERE course_id=" + courseId
                                    + " AND course_role='Instructor' AND active=1 FOR UPDATE").executeQuery();
                    if (!rs.next()) {
                        conn.rollback();
                        fail.incrementAndGet();
                        return null;
                    }
                    int oldId = rs.getInt(1);
                    rs.close();
                    conn.prepareStatement("UPDATE enrollment SET active=0 WHERE id=" + oldId).executeUpdate();
                    conn.prepareStatement(
                            "INSERT INTO enrollment (course_id,user_id,course_role,can_grade,can_post_announcements,"
                                    + "can_manage_groups,can_manage_course_events,active,enrolled_at) VALUES ("
                                    + courseId + "," + newInstructorId
                                    + ",'Instructor',1,1,1,1,1,UTC_TIMESTAMP())").executeUpdate();
                    conn.prepareStatement("UPDATE course SET instructor_id=" + newInstructorId + " WHERE id=" + courseId)
                            .executeUpdate();
                    conn.commit();
                    success.incrementAndGet();
                } catch (Exception e) {
                    conn.rollback();
                    fail.incrementAndGet();
                } finally {
                    conn.setAutoCommit(true);
                }
                return null;
            });
        } catch (Exception e) {
            fail.incrementAndGet();
        }
    }

    private boolean isEmpty(Integer courseId) {
        Integer extra = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollment WHERE course_id=? AND NOT (course_role='Instructor' AND active=1)",
                Integer.class, courseId);
        Integer weeks = jdbc.queryForObject("SELECT COUNT(*) FROM course_week WHERE course_id=?", Integer.class, courseId);
        Integer assignments = jdbc.queryForObject("SELECT COUNT(*) FROM assignment WHERE course_id=?", Integer.class, courseId);
        Integer quizzes = jdbc.queryForObject("SELECT COUNT(*) FROM quiz WHERE course_id=?", Integer.class, courseId);
        Integer groups = jdbc.queryForObject("SELECT COUNT(*) FROM group_set WHERE course_id=?", Integer.class, courseId);
        Integer announcements = jdbc.queryForObject("SELECT COUNT(*) FROM course_announcement WHERE course_id=?", Integer.class, courseId);
        return (extra == null || extra == 0)
                && (weeks == null || weeks == 0)
                && (assignments == null || assignments == 0)
                && (quizzes == null || quizzes == 0)
                && (groups == null || groups == 0)
                && (announcements == null || announcements == 0);
    }

    private Integer insertEmptyCourse(int id, int instructorId) {
        jdbc.update("DELETE FROM enrollment WHERE course_id=?", id);
        jdbc.update("DELETE FROM course WHERE id=?", id);
        jdbc.update("INSERT INTO course (id,tenant_id,course_code,title,term_start_date,term_end_date,instructor_id,creator_id,state) "
                        + "VALUES (?,?,?,?, '2026-01-01','2026-06-01',?,?,'Active')",
                id, 1, "C" + id, "Course " + id, instructorId, instructorId);
        jdbc.update("INSERT INTO enrollment (course_id,user_id,course_role,can_grade,can_post_announcements,"
                        + "can_manage_groups,can_manage_course_events,active,enrolled_at) "
                        + "VALUES (?,?, 'Instructor',1,1,1,1,1,UTC_TIMESTAMP())",
                id, instructorId);
        return id;
    }

    private void applySchema(DataSource ds) {
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
                  description TEXT NULL,
                  location VARCHAR(255) NULL,
                  instructor_id INT NOT NULL,
                  state ENUM('Active','Archived') NOT NULL DEFAULT 'Active',
                  archived_at DATETIME NULL,
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
                  instructor_course_id INT GENERATED ALWAYS AS (
                    IF(course_role = 'Instructor' AND active = 1, course_id, NULL)
                  ) STORED,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_enrollment_course_user (course_id, user_id),
                  UNIQUE KEY uk_enrollment_one_instructor (instructor_course_id)
                );
                CREATE TABLE course_week (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE course_material (
                  id INT NOT NULL AUTO_INCREMENT,
                  week_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE course_syllabus (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE course_session (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE course_event (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE assignment (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE quiz (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE group_set (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                CREATE TABLE course_announcement (
                  id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  PRIMARY KEY (id)
                );
                """.getBytes(StandardCharsets.UTF_8)));
        pop.execute(ds);
    }

    private void seed() {
        jdbc.update("INSERT INTO tenant (id,name,timezone,status) VALUES (1,'T1','UTC','ACTIVE')");
        jdbc.update("INSERT INTO `user` (id,tenant_id,username,password,email,role,level,status) VALUES "
                + "(1,1,'u1','x','u1@ex.com','USER','INSTRUCTOR','ACTIVE'),"
                + "(2,1,'u2','x','u2@ex.com','USER','INSTRUCTOR','ACTIVE'),"
                + "(3,1,'u3','x','u3@ex.com','USER','STUDENT','ACTIVE'),"
                + "(4,1,'u4','x','u4@ex.com','USER','INSTRUCTOR','ACTIVE')");
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
}
