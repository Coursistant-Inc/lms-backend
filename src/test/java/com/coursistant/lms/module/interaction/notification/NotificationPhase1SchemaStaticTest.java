package com.coursistant.lms.module.interaction.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPhase1SchemaStaticTest {

    @Test
    void mapperColumns_areSubsetOfDdl() throws Exception {
        String ddl = Files.readString(Path.of("sql/notification_phase1.sql"));
        assertColumnsCovered(ddl, "notification_event_outbox",
                "src/main/resources/mapper/interaction/NotificationEventOutboxMapper.xml");
        assertColumnsCovered(ddl, "notification_delivery",
                "src/main/resources/mapper/interaction/NotificationDeliveryMapper.xml");
        assertColumnsCovered(ddl, "notification_digest_email",
                "src/main/resources/mapper/interaction/NotificationDigestEmailMapper.xml");
        assertTrue(ddl.contains("uk_outbox_event"));
        assertTrue(ddl.contains("uk_delivery_dedupe"));
        assertTrue(ddl.contains("uk_digest_email"));
        assertFalse(ddl.toLowerCase(Locale.ROOT).contains("alter table user_notification"));
    }

    @Test
    void claimUpdate_doesNotClearSendAttemptedAt() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/interaction/NotificationDeliveryMapper.xml"));
        int start = xml.indexOf("<update id=\"claim\">");
        int end = xml.indexOf("</update>", start);
        String claim = xml.substring(start, end).toLowerCase(Locale.ROOT);
        assertFalse(claim.contains("send_attempted_at"));
        assertTrue(claim.contains("attempt_count = attempt_count + 1"));
    }

    @Test
    void markRetry_clearsSendAttemptedAt() throws Exception {
        for (String path : List.of(
                "src/main/resources/mapper/interaction/NotificationDeliveryMapper.xml",
                "src/main/resources/mapper/interaction/NotificationDigestEmailMapper.xml")) {
            String xml = Files.readString(Path.of(path));
            int start = xml.indexOf("<update id=\"markRetry\">");
            int end = xml.indexOf("</update>", start);
            String sql = xml.substring(start, end).toLowerCase(Locale.ROOT);
            assertTrue(sql.contains("send_attempted_at = null"), path);
        }
    }

    @Test
    void markSendAttempted_renewsLease() throws Exception {
        for (String path : List.of(
                "src/main/resources/mapper/interaction/NotificationDeliveryMapper.xml",
                "src/main/resources/mapper/interaction/NotificationDigestEmailMapper.xml")) {
            String xml = Files.readString(Path.of(path));
            int start = xml.indexOf("<update id=\"markSendAttempted\">");
            int end = xml.indexOf("</update>", start);
            String sql = xml.substring(start, end).toLowerCase(Locale.ROOT);
            assertTrue(sql.contains("lease_until = #{leaseuntil}"), path);
            assertTrue(sql.contains("lease_until &gt; #{now}") || sql.contains("lease_until > #{now}"), path);
        }
    }

    @Test
    void digestClaimBatch_excludesCollecting() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/interaction/NotificationDigestEmailMapper.xml"));
        int start = xml.indexOf("<select id=\"selectClaimBatch\"");
        int end = xml.indexOf("</select>", start);
        String sql = xml.substring(start, end);
        assertTrue(sql.contains("PENDING"));
        assertTrue(sql.contains("FAILED_RETRYABLE"));
        assertTrue(sql.contains("PROCESSING"));
        assertTrue(sql.contains("digest_date"));
        assertTrue(sql.contains("tenant_id"));
        assertFalse(sql.contains("COLLECTING"));
        assertFalse(sql.toLowerCase(Locale.ROOT).contains("skip locked"));
    }

    @Test
    void claimBatches_areCandidateSelectsWithoutSkipLocked() throws Exception {
        for (String path : List.of(
                "src/main/resources/mapper/interaction/NotificationEventOutboxMapper.xml",
                "src/main/resources/mapper/interaction/NotificationDeliveryMapper.xml",
                "src/main/resources/mapper/interaction/NotificationDigestEmailMapper.xml")) {
            String xml = Files.readString(Path.of(path));
            int start = xml.indexOf("<select id=\"selectClaimBatch\"");
            int end = xml.indexOf("</select>", start);
            String sql = xml.substring(start, end).toLowerCase(Locale.ROOT);
            assertFalse(sql.contains("skip locked"), path);
            assertFalse(sql.contains("for update"), path);
        }
    }

    @Test
    void phase1ScriptsExist() throws Exception {
        assertTrue(Files.exists(Path.of("sql/notification_phase1.sql")));
        assertTrue(Files.exists(Path.of("sql/notification_phase1_gate_check.sql")));
        assertTrue(Files.exists(Path.of("sql/notification_phase1_stats.sql")));
        assertTrue(Files.exists(Path.of("sql/notification_phase1_drop.sql")));
        String gate = Files.readString(Path.of("sql/notification_phase1_gate_check.sql"));
        assertTrue(gate.contains("COUNT(DISTINCT index_name)"));
        assertTrue(gate.contains("digest_terminal_parent_processing_children"));
        assertTrue(gate.contains("overdue_collecting"));
        assertTrue(gate.contains("long_pending_outbox"));
        String stats = Files.readString(Path.of("sql/notification_phase1_stats.sql"));
        assertTrue(stats.contains("user_notification"));
        assertTrue(stats.contains("channel <> 'IN_APP'") || stats.contains("channel != 'IN_APP'"));
    }

    private static void assertColumnsCovered(String ddl, String table, String mapperPath) throws Exception {
        Set<String> ddlCols = ddlColumns(ddl, table);
        Set<String> mapperCols = mapperColumns(Files.readString(Path.of(mapperPath)));
        List<String> missing = new ArrayList<>();
        for (String col : mapperCols) {
            if (!ddlCols.contains(col)) {
                missing.add(table + "." + col);
            }
        }
        assertTrue(missing.isEmpty(), "Mapper columns missing from DDL: " + missing);
    }

    private static Set<String> ddlColumns(String ddl, String table) {
        Pattern tablePat = Pattern.compile(
                "CREATE TABLE IF NOT EXISTS " + table + " \\((.*?)\\) ENGINE",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = tablePat.matcher(ddl);
        assertTrue(m.find(), "missing table " + table);
        Set<String> cols = new LinkedHashSet<>();
        for (String line : m.group(1).split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("PRIMARY") || trimmed.startsWith("UNIQUE")
                    || trimmed.startsWith("KEY ") || trimmed.startsWith(")")) {
                continue;
            }
            String name = trimmed.split("\\s+")[0].replace("`", "").toLowerCase(Locale.ROOT);
            if (!name.isBlank()) {
                cols.add(name);
            }
        }
        return cols;
    }

    private static Set<String> mapperColumns(String xml) {
        int start = xml.indexOf("<sql id=\"Base_Column_List\">");
        int end = xml.indexOf("</sql>", start);
        String list = xml.substring(start, end);
        Set<String> cols = new LinkedHashSet<>();
        Matcher m = Pattern.compile("([a-z_]+)(?:\\s+AS\\s+\\w+)?", Pattern.CASE_INSENSITIVE).matcher(list);
        while (m.find()) {
            String col = m.group(1).toLowerCase(Locale.ROOT);
            if (!Set.of("sql", "id", "as").contains(col) && !col.startsWith("base")) {
                cols.add(col);
            }
        }
        cols.remove("base_column_list");
        return cols;
    }
}
