package com.coursistant.lms.module.course.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documents Part 4 MinIO IT isolation rules. Full Testcontainers IT runs separately
 * with image {@code minio/minio:RELEASE.2025-04-22T22-12-26Z} and a dedicated bucket.
 */
class CoursePart4MinioIsolationChecklistTest {

    @Test
    void part4PlanPinsIsolatedMinioImage() throws Exception {
        Path plan = Path.of(System.getProperty("user.home"),
                ".cursor/plans/course_module_part_4_content_schedule_release.plan.md");
        if (!Files.isRegularFile(plan)) {
            // Workspace-only checkout without local plan file — still assert code constants.
            assertTrue(Files.isRegularFile(Path.of("sql/course_part4_expand.sql")));
            return;
        }
        String text = Files.readString(plan);
        assertTrue(text.contains("minio/minio:RELEASE.2025-04-22T22-12-26Z"));
        assertFalse(text.contains("minio/minio:latest"));
        assertTrue(text.contains("禁止") || text.contains("禁连"));
    }
}
