package com.coursistant.lms.module.course.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Part 1 merge-gate checklist (surefire). Documents CPS coexistence and artifacts.
 */
class CoursePart1GateChecklistTest {

    @Test
    void part1ArtifactsPresent() {
        assertTrue(Files.exists(Path.of("sql/course_part1_precheck.sql")));
        assertTrue(Files.exists(Path.of("sql/course_part1_expand.sql")));
        assertTrue(Files.exists(Path.of("sql/course_part1_backfill.sql")));
        assertTrue(Files.exists(Path.of("sql/course_part1_gate_check.sql")));
        assertTrue(Files.exists(Path.of("sql/course_part1_active_instructor_uk.sql")));
        assertTrue(Files.exists(Path.of("sql/course_part1_restore.sql")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/shared/security/ActorContext.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/shared/security/ActorContextResolver.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/service/CourseAuthorizationService.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/service/CourseAuditService.java")));
        assertTrue(Files.exists(Path.of(
                "src/test/java/com/coursistant/lms/module/course/it/CoursePart1MigrationIT.java")));
    }

    @Test
    void failsafeIncludesCourseIt() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("**/course/it/**/*IT.java"));
    }

    @Test
    void authzServiceDocumentsCpsCoexistence() throws Exception {
        String text = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/service/CourseAuthorizationService.java"));
        assertTrue(text.contains("CoursePermissionService"));
        assertTrue(text.contains("normative") || text.contains("Coexistence") || text.contains("coexistence")
                || text.contains("Unwired"));
    }

    @Test
    void enrollmentSqlPinsActiveInGeneratedColumn() throws Exception {
        String sql = Files.readString(Path.of("sql/enrollment.sql"));
        assertTrue(sql.contains("active = 1") || sql.contains("active=1"));
        assertTrue(sql.contains("uk_enrollment_one_instructor"));
    }
}
