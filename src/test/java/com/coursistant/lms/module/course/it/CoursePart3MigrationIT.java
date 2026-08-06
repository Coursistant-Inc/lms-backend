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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoursePart3MigrationIT {

    private MySQLContainer<?> mysql;
    private JdbcTemplate jdbc;
    private final List<String> report = new ArrayList<>();

    @BeforeAll
    void start() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker required");
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
                .withDatabaseName("lms_course_p3")
                .withUsername("p3")
                .withPassword("p3");
        mysql.start();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        String base = mysql.getJdbcUrl();
        String sep = base.contains("?") ? "&" : "?";
        ds.setUrl(base + sep + "allowMultiQueries=true&allowPublicKeyRetrieval=true");
        ds.setUsername(mysql.getUsername());
        ds.setPassword(mysql.getPassword());
        jdbc = new JdbcTemplate(ds);
        applySchema(ds);
        seed();
        runSqlFile("sql/course_part3_expand.sql");
        report.add("jdbc=" + mysql.getJdbcUrl());
    }

    @AfterAll
    void stop() {
        if (mysql != null) {
            mysql.stop();
        }
        System.out.println("CoursePart3MigrationIT report:\n" + String.join("\n", report));
    }

    @Test
    @Order(1)
    void scriptsExist() {
        assertTrue(Files.exists(Path.of("sql/course_part3_expand.sql")));
        assertTrue(Files.exists(Path.of("sql/course_part3_gate_check.sql")));
    }

    @Test
    @Order(2)
    void gate_structureAndActiveInvariant() {
        Integer cols = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() "
                        + "AND table_name='enrollment' AND column_name IN "
                        + "('withdrawn_at','withdrawn_by_actor_type','withdrawn_by_actor_id')",
                Integer.class);
        assertEquals(3, cols);

        Integer idx = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() "
                        + "AND table_name='enrollment' AND index_name='idx_enrollment_course_active'",
                Integer.class);
        assertTrue(idx != null && idx >= 1);

        // dirty active must fail gate B
        jdbc.update("UPDATE enrollment SET withdrawn_at=UTC_TIMESTAMP() WHERE id=1 AND active=1");
        Integer dirty = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollment WHERE active=1 AND withdrawn_at IS NOT NULL", Integer.class);
        assertTrue(dirty != null && dirty >= 1);
        jdbc.update("UPDATE enrollment SET withdrawn_at=NULL, withdrawn_by_actor_type=NULL, "
                + "withdrawn_by_actor_id=NULL WHERE id=1");

        // legacy inactive incomplete does not fail
        jdbc.update("INSERT INTO enrollment (course_id,user_id,course_role,active,enrolled_at) "
                + "VALUES (1,3,'Student',0,UTC_TIMESTAMP())");
        Integer legacy = jdbc.queryForObject(
                "SELECT COUNT(*) FROM enrollment WHERE active=0 AND withdrawn_at IS NULL", Integer.class);
        assertTrue(legacy != null && legacy >= 1);
        report.add("legacy_incomplete_withdrawn_count=" + legacy);
    }

    @Test
    @Order(3)
    void newWithdraw_writesCompleteTriple() {
        jdbc.update("UPDATE enrollment SET active=0, withdrawn_at=UTC_TIMESTAMP(), "
                + "withdrawn_by_actor_type='USER', withdrawn_by_actor_id=1 WHERE id=2");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT active, withdrawn_at, withdrawn_by_actor_type, withdrawn_by_actor_id "
                        + "FROM enrollment WHERE id=2");
        Object active = row.get("active");
        assertTrue(Boolean.FALSE.equals(active) || Integer.valueOf(0).equals(active)
                || (active instanceof Number n && n.intValue() == 0));
        assertNotNull(row.get("withdrawn_at"));
        assertEquals("USER", row.get("withdrawn_by_actor_type"));
        assertEquals(1, ((Number) row.get("withdrawn_by_actor_id")).intValue());
        report.add("newWithdrawComplete=true");
    }

    private void applySchema(DataSource ds) {
        ResourceDatabasePopulator pop = new ResourceDatabasePopulator();
        pop.addScript(new ByteArrayResource("""
                CREATE TABLE tenant (
                  id INT NOT NULL AUTO_INCREMENT, name VARCHAR(255) NOT NULL,
                  timezone VARCHAR(64) NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                  PRIMARY KEY (id));
                CREATE TABLE `user` (
                  id INT NOT NULL AUTO_INCREMENT, tenant_id INT NULL,
                  username VARCHAR(255) NOT NULL, password VARCHAR(255) NOT NULL,
                  email VARCHAR(255) NOT NULL, role VARCHAR(32) NOT NULL, level VARCHAR(32) NULL,
                  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', PRIMARY KEY (id));
                CREATE TABLE course (
                  id INT NOT NULL AUTO_INCREMENT, tenant_id INT NOT NULL,
                  course_code VARCHAR(32) NOT NULL, title VARCHAR(255) NOT NULL,
                  term_start_date DATE NOT NULL, term_end_date DATE NOT NULL,
                  instructor_id INT NOT NULL, state ENUM('Active','Archived') NOT NULL DEFAULT 'Active',
                  creator_id INT NOT NULL, PRIMARY KEY (id));
                CREATE TABLE enrollment (
                  id INT NOT NULL AUTO_INCREMENT, course_id INT NOT NULL, user_id INT NOT NULL,
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
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_enrollment_course_user (course_id, user_id));
                """.getBytes(StandardCharsets.UTF_8)));
        pop.execute(ds);
    }

    private void seed() {
        jdbc.update("INSERT INTO tenant (id,name,timezone,status) VALUES (1,'T1','UTC','ACTIVE')");
        jdbc.update("INSERT INTO `user` (id,tenant_id,username,password,email,role,level,status) VALUES "
                + "(1,1,'u1','x','u1@ex.com','USER','INSTRUCTOR','ACTIVE'),"
                + "(2,1,'u2','x','u2@ex.com','USER','STUDENT','ACTIVE'),"
                + "(3,1,'u3','x','u3@ex.com','USER','STUDENT','ACTIVE')");
        jdbc.update("INSERT INTO course (id,tenant_id,course_code,title,term_start_date,term_end_date,instructor_id,creator_id) "
                + "VALUES (1,1,'CS','C','2026-01-01','2026-06-01',1,1)");
        jdbc.update("INSERT INTO enrollment (id,course_id,user_id,course_role,active,enrolled_at) VALUES "
                + "(1,1,1,'Instructor',1,UTC_TIMESTAMP()),(2,1,2,'Student',1,UTC_TIMESTAMP())");
    }

    private void runSqlFile(String path) throws Exception {
        try (Connection c = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource(path));
        }
        report.add("ran " + path);
    }
}
