package com.coursistant.lms.module.interaction.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPhase2SchemaStaticTest {

    @Test
    void phase2ScriptsExistAndAreIdempotent() throws Exception {
        Path ddl = Path.of("sql/notification_phase2.sql");
        Path gate = Path.of("sql/notification_phase2_gate_check.sql");
        assertTrue(Files.exists(ddl));
        assertTrue(Files.exists(gate));
        String sql = Files.readString(ddl).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("publication_version"));
        assertTrue(sql.contains("schedule_version"));
        assertFalse(sql.contains("add column if not exists"));
        assertFalse(sql.contains("alter table assignment drop"));
        assertFalse(sql.contains("alter table quiz drop"));
        assertFalse(sql.contains("alter table course_week drop"));
        String gateSql = Files.readString(gate).toLowerCase(Locale.ROOT);
        assertTrue(gateSql.contains("assignment_publication_version"));
        assertTrue(gateSql.contains("quiz_publication_version"));
        assertTrue(gateSql.contains("course_week_publication_version"));
    }

    @Test
    void statsAndRolloutNotesExist() throws Exception {
        Path stats = Path.of("sql/notification_phase2_stats.sql");
        assertTrue(Files.exists(stats));
        String sql = Files.readString(stats);
        assertTrue(sql.contains("empty_snapshot_count"));
        assertTrue(sql.contains("unexpected_immediate_email"));
        assertTrue(sql.contains("type_subject_mismatch"));
        assertTrue(sql.contains("subject_course_mismatch"));
        assertTrue(sql.contains("empty_suspect"));
        String ddl = Files.readString(Path.of("sql/notification_phase2.sql")).toLowerCase(Locale.ROOT);
        assertTrue(ddl.contains("do not mix"));
    }

    @Test
    void productionCode_doesNotIntroduceQuizExtension() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/interaction/notification/enums/NotificationType.java"));
        assertFalse(src.contains("QUIZ_EXTENSION_CHANGED"));
        String tree = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/interaction/notification/service/NotificationPolicy.java"));
        assertFalse(tree.contains("default ->"));
    }

    @Test
    void newProducers_doNotWriteCourseActiveStudents() throws Exception {
        for (String path : java.util.List.of(
                "src/main/java/com/coursistant/lms/module/course/announcement/service/CourseAnnouncementService.java",
                "src/main/java/com/coursistant/lms/module/assignment/service/AssignmentNotificationService.java",
                "src/main/java/com/coursistant/lms/module/quiz/service/QuizNotificationService.java",
                "src/main/java/com/coursistant/lms/module/course/content/week/service/CourseWeekService.java",
                "src/main/java/com/coursistant/lms/module/course/event/service/CourseEventService.java",
                "src/main/java/com/coursistant/lms/module/course/group/service/GroupNotificationService.java")) {
            String src = Files.readString(Path.of(path));
            assertFalse(src.contains("COURSE_ACTIVE_STUDENTS"), path);
        }
    }

    @Test
    void guardedPublicationSql_isNotOnGenericUpdate() throws Exception {
        String assignmentXml = Files.readString(Path.of("src/main/resources/mapper/assignment/AssignmentMapper.xml"));
        int generic = assignmentXml.indexOf("<update id=\"updateById\"");
        int genericEnd = assignmentXml.indexOf("</update>", generic);
        String genericSql = assignmentXml.substring(generic, genericEnd);
        assertFalse(genericSql.contains("publication_version"));
        assertFalse(genericSql.contains("schedule_version"));
        assertTrue(assignmentXml.contains("publishAndIncrementPublicationVersion"));
        String quizXml = Files.readString(Path.of("src/main/resources/mapper/quiz/QuizMapper.xml"));
        int quizUpdate = quizXml.indexOf("<update id=\"updateById\"");
        int quizEnd = quizXml.indexOf("</update>", quizUpdate);
        assertFalse(quizXml.substring(quizUpdate, quizEnd).contains("publication_version"));
    }
}
