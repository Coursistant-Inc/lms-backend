package com.coursistant.lms.module.interaction.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CI-always static contract checks for notification schema / mapper. Does not replace real migration.
 */
class NotificationSchemaStaticTest {

    @Test
    void insertChunk_omitsLegacyColumns() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/interaction/UserNotificationMapper.xml"));
        int insertStart = xml.indexOf("<insert id=\"insertChunk\"");
        assertTrue(insertStart >= 0, "insertChunk missing");
        int insertEnd = xml.indexOf("</insert>", insertStart);
        String insertChunk = xml.substring(insertStart, insertEnd);
        assertFalse(insertChunk.contains("event_type"), insertChunk);
        assertFalse(insertChunk.contains("ref_id"), insertChunk);
        assertFalse(insertChunk.contains("title"), insertChunk);
        assertTrue(insertChunk.contains("notification_type"));
        assertTrue(insertChunk.contains("event_key"));
    }

    @Test
    void gateCheck_assertsLegacyColumnsAbsent() throws Exception {
        String sql = Files.readString(Path.of("sql/notification_v1_gate_check.sql"));
        assertTrue(sql.contains("legacy_columns_remaining"));
        assertTrue(sql.contains("'event_type'"));
        assertTrue(sql.contains("'ref_id'"));
        assertTrue(sql.contains("'title'"));
    }

    @Test
    void dropLegacyColumnsSql_usesCanonicalProcedure() throws Exception {
        String drop = Files.readString(Path.of("sql/notification_v1_drop_legacy_columns.sql"));
        assertTrue(drop.contains("CREATE PROCEDURE drop_user_notification_legacy_columns"));
        assertTrue(drop.contains("DROP COLUMN event_type"));
        assertTrue(drop.contains("DROP COLUMN ref_id"));
        assertTrue(drop.contains("DROP COLUMN title"));
        assertTrue(drop.contains("CALL drop_user_notification_legacy_columns()"));

        String migration = Files.readString(Path.of("sql/notification_v1.sql"));
        assertTrue(migration.contains("CREATE PROCEDURE drop_user_notification_legacy_columns"));
        assertTrue(migration.contains("DROP COLUMN event_type"));
        assertTrue(migration.contains("CALL drop_user_notification_legacy_columns()"));
        assertFalse(migration.toLowerCase().contains("source "), "must inline DROP procedure, not SOURCE");
    }
}
