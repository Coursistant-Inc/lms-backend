package com.coursistant.lms.module.interaction.notification.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationDeliveryDedupeIT {

    @BeforeAll
    static void start() {
        NotificationPhase1Mysql.ensureStarted();
    }

    @Test
    void concurrentUpsert_sameDedupeKey_oneRow() throws Exception {
        JdbcTemplate jdbc = NotificationPhase1Mysql.jdbc();
        String eventId = NotificationPhase1Mysql.uuid();
        String eventKey = "dedupe-" + eventId;
        String sql = """
                INSERT INTO notification_delivery (
                  event_id, tenant_id, recipient_user_id, course_id, notification_type, subject_type,
                  subject_id, event_key, channel, status, message, deep_link, occurred_at,
                  attempt_count, next_attempt_at, unknown_outcome_count, created_at, updated_at
                ) VALUES (?, 1, 4, 2, 'ASSIGNMENT_GRADE_RELEASED', 'ASSIGNMENT', 9, ?, 'IN_APP', 'SENT',
                  'msg', '/x', UTC_TIMESTAMP(3), 0, UTC_TIMESTAMP(3), 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                ON DUPLICATE KEY UPDATE id = id
                """;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> jdbc.update(sql, eventId, eventKey));
        }
        List<Future<Integer>> futures = pool.invokeAll(tasks);
        int sum = 0;
        for (Future<Integer> future : futures) {
            sum += future.get();
        }
        pool.shutdownNow();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_delivery WHERE event_id = ? AND channel = 'IN_APP'",
                Integer.class, eventId);
        assertEquals(1, count);
        assertEquals(true, sum >= 1);
    }
}
