package com.coursistant.lms.module.auth.it;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins production-aligned container image tags used by Auth IT (todo p2-testcontainers).
 */
class AuthItImageDefaultsTest {

    @Test
    void authItUsesPinnedMysqlAndRedisImages() throws Exception {
        Path base = Path.of("src/test/java/com/coursistant/lms/module/auth/it/support/AuthIntegrationTestBase.java");
        assertTrue(Files.exists(base));
        String text = Files.readString(base);
        assertTrue(text.contains("mysql:8.0.36"), "MySQL image must be pinned to 8.0.36");
        assertTrue(text.contains("redis:7.2-alpine"), "Redis image must be pinned to 7.2-alpine");
    }

    @Test
    void productionYaml_keepsAuthTtlDefaults() throws Exception {
        Path yml = Path.of("src/main/resources/application.yml");
        String text = Files.readString(yml);
        assertTrue(text.contains("access-expire-hours: 2"));
        assertTrue(text.contains("refresh-expire-days: 14"));
        assertTrue(text.contains("refresh-rotation-grace-seconds: 30"));
    }
}
