package com.coursistant.lms.module.auth.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Documents Merge Gate checklist for Auth phase-2/3 (executed via failsafe *IT).
 */
class AuthMergeGateChecklistTest {

    @Test
    void failsafeIncludesAuthItClasses() throws Exception {
        Path pom = Path.of("pom.xml");
        String text = Files.readString(pom);
        assertTrue(text.contains("maven-failsafe-plugin"));
        assertTrue(text.contains("*IT.java"));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/RefreshTokenConcurrencyIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthLoginIT.java")));
        assertTrue(Files.exists(Path.of("src/test/resources/application-auth-it.yml")));
        assertTrue(Files.exists(Path.of("src/test/resources/auth-it-schema.sql")));
    }

    @Test
    void phase3LocalArtifactsPresent() {
        assertTrue(Files.exists(Path.of("src/test/resources/application-auth-phase3-local.yml")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/support/AuthContainerFaults.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthRedisFailureIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthDatabaseFailureIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthConsistencyIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthMigrationIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthEnumerationIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthSensitiveLoggingIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthAuditIT.java")));
        assertTrue(Files.exists(Path.of("src/test/java/com/coursistant/lms/module/auth/it/AuthLocalPerformanceIT.java")));
        assertTrue(Files.exists(Path.of(
                "src/main/java/com/coursistant/lms/module/auth/token/service/RefreshTokenReusedException.java")));
    }
}
