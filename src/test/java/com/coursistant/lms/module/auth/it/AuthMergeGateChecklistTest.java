package com.coursistant.lms.module.auth.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Documents Merge Gate checklist for Auth phase-2 (executed via failsafe *IT).
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
}
