package com.coursistant.lms.module.auth.unit;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards production default TTL / JWT lifetime values from accidental drift.
 */
class ProductionAuthDefaultsTest {

    @Test
    void productionYaml_keepsAuthHardeningDefaults() throws Exception {
        Path yml = Path.of("src/main/resources/application.yml");
        assertTrue(Files.exists(yml), "application.yml must exist for default TTL assertion");
        String text = Files.readString(yml);
        assertTrue(text.contains("access-expire-hours: 2"), text);
        assertTrue(text.contains("refresh-expire-days: 14"), text);
        assertTrue(text.contains("refresh-rotation-grace-seconds: 30"), text);
        assertTrue(text.contains("issuer:"), text);
        assertTrue(text.contains("audience:"), text);
    }

    @Test
    void authTestProfile_usesTestRsaKeysOnly() throws Exception {
        Path yml = Path.of("src/test/resources/application-auth-test.yml");
        assertTrue(Files.exists(yml));
        String text = Files.readString(yml);
        assertTrue(text.contains("classpath:test-private.pem"));
        assertTrue(text.contains("classpath:test-public.pem"));
        assertFalse(text.contains("private.pem\n") && !text.contains("test-private"));
    }
}
