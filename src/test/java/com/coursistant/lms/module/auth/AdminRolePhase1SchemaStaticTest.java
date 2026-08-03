package com.coursistant.lms.module.auth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdminRolePhase1SchemaStaticTest {

    @Test
    void phase1SqlScriptsExistAndContainCutover() throws Exception {
        Path migration = Path.of("sql/admin_role_phase1.sql");
        Path precheck = Path.of("sql/admin_role_phase1_precheck.sql");
        Path gate = Path.of("sql/admin_role_phase1_gate_check.sql");
        Path restore = Path.of("sql/admin_role_phase1_restore.sql");
        assertTrue(Files.exists(migration));
        assertTrue(Files.exists(precheck));
        assertTrue(Files.exists(gate));
        assertTrue(Files.exists(restore));
        String sql = Files.readString(migration);
        assertTrue(sql.contains("SYSTEM_ADMIN"));
        assertTrue(sql.contains("DELETE FROM refresh_tokens"));
        assertFalse(sql.toLowerCase().contains("set level = 'student'"));
    }
}
