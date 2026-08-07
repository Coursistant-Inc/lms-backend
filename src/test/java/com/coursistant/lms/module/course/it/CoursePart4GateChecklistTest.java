package com.coursistant.lms.module.course.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoursePart4GateChecklistTest {

    @Test
    void expandAndGateScriptsPresent() throws Exception {
        Path expand = Path.of("sql/course_part4_expand.sql");
        Path gate = Path.of("sql/course_part4_gate_check.sql");
        assertTrue(Files.isRegularFile(expand));
        assertTrue(Files.isRegularFile(gate));
        String expandSql = Files.readString(expand);
        assertTrue(expandSql.contains("upload_operation"));
        assertTrue(expandSql.contains("minio_object_outbox"));
        assertTrue(expandSql.contains("active_dedupe_key"));
        String gateSql = Files.readString(gate);
        assertTrue(gateSql.contains("upload_operation_exists"));
        assertTrue(gateSql.contains("minio_outbox_exists"));
    }

    @Test
    void multipartFingerprintHelperExists() {
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/coursistant/lms/shared/idempotency/MultipartFingerprint.java")));
    }
}
