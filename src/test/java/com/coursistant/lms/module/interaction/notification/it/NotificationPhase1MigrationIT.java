package com.coursistant.lms.module.interaction.notification.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationPhase1MigrationIT {

    @BeforeAll
    static void start() {
        NotificationPhase1Mysql.ensureStarted();
    }

    @Test
    void scriptsAreIdempotentAndGateCheckPasses() {
        NotificationPhase1Mysql.runSql("sql/notification_phase1.sql");
        NotificationPhase1Mysql.runSql("sql/notification_phase1.sql");
        JdbcTemplate jdbc = NotificationPhase1Mysql.jdbc();
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'notification_event_outbox') AS outbox_table,
                  (SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'notification_event_recipient') AS recipient_table,
                  (SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'notification_delivery') AS delivery_table,
                  (SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'notification_digest_email') AS digest_email_table,
                  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = 'notification_event_outbox'
                      AND index_name = 'uk_outbox_event') AS uk_outbox_event,
                  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = 'notification_delivery'
                      AND index_name = 'uk_delivery_dedupe') AS uk_delivery_dedupe,
                  (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'notification_delivery'
                      AND column_name IN ('claim_token', 'send_attempted_at', 'unknown_outcome_count')) AS delivery_claim_columns
                """);
        assertEquals(1L, ((Number) row.get("outbox_table")).longValue());
        assertEquals(1L, ((Number) row.get("recipient_table")).longValue());
        assertEquals(1L, ((Number) row.get("delivery_table")).longValue());
        assertEquals(1L, ((Number) row.get("digest_email_table")).longValue());
        assertEquals(1L, ((Number) row.get("uk_outbox_event")).longValue());
        assertEquals(1L, ((Number) row.get("uk_delivery_dedupe")).longValue());
        assertEquals(3L, ((Number) row.get("delivery_claim_columns")).longValue());
    }
}
