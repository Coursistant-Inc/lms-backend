package com.coursistant.lms.module.course.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoursePart3GateChecklistTest {

    @Test
    void part3AmendmentArtifactsPresent() {
        assertTrue(Files.exists(Path.of("sql/course_part3_expand.sql")));
        assertTrue(Files.exists(Path.of("sql/course_part3_gate_check.sql")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/enrollment/service/EnrollmentMembershipService.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/enrollment/service/EnrollmentBatchItemService.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/enrollment/service/EnrollmentIdentityGuard.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/enrollment/controller/CourseStudentController.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/course/enrollment/controller/CourseTaController.java")));
    }
}
