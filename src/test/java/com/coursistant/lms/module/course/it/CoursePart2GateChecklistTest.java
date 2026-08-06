package com.coursistant.lms.module.course.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Surefire gate: Part 2 deliverables exist. */
class CoursePart2GateChecklistTest {

    @Test
    void part2ArtifactsPresent() {
        assertTrue(Files.exists(Path.of("sql/course_part2_expand.sql")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/service/CourseLifecycleSupport.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/service/CourseAuditActions.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/dto/PatchCourseRequest.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/dto/ReassignPrimaryInstructorRequest.java")));
        assertTrue(Files.exists(Path.of(
                "src/test/java/com/coursistant/lms/module/course/course/service/CourseServiceCoreTest.java")));
        assertTrue(Files.exists(Path.of(
                "src/test/java/com/coursistant/lms/module/course/it/CoursePart2CoreIT.java")));
        assertTrue(Files.notExists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/course/dto/TransferInstructorRequest.java")));
    }
}
