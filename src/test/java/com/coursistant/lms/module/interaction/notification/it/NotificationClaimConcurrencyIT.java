package com.coursistant.lms.module.interaction.notification.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationClaimConcurrencyIT {

    @BeforeAll
    static void start() {
        NotificationPhase1Mysql.ensureStarted();
    }

    @Test
    void twoWorkers_onlyOneClaimsRow() throws Exception {
        JdbcTemplate jdbc = NotificationPhase1Mysql.jdbc();
        String eventId = NotificationPhase1Mysql.uuid();
        long id = NotificationPhase1Mysql.insertDelivery(eventId, 4, "ASSIGNMENT_SUBMISSION_RECEIVED",
                "ASSIGNMENT_SUBMISSION", 9, "claim-" + eventId, "IMMEDIATE_EMAIL", "PENDING");
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CallableClaim c1 = new CallableClaim(id, start, winners);
        CallableClaim c2 = new CallableClaim(id, start, winners);
        Future<Integer> f1 = pool.submit(c1);
        Future<Integer> f2 = pool.submit(c2);
        start.countDown();
        int a = f1.get(10, TimeUnit.SECONDS);
        int b = f2.get(10, TimeUnit.SECONDS);
        pool.shutdownNow();
        assertEquals(1, a + b);
        assertEquals(1, winners.get());
        Integer processing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification_delivery WHERE id = ? AND status = 'PROCESSING'",
                Integer.class, id);
        assertEquals(1, processing);
    }

    private static final class CallableClaim implements java.util.concurrent.Callable<Integer> {
        private final long id;
        private final CountDownLatch start;
        private final AtomicInteger winners;

        private CallableClaim(long id, CountDownLatch start, AtomicInteger winners) {
            this.id = id;
            this.start = start;
            this.winners = winners;
        }

        @Override
        public Integer call() throws Exception {
            start.await(5, TimeUnit.SECONDS);
            try (Connection c = NotificationPhase1Mysql.dataSource().getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         UPDATE notification_delivery
                         SET status = 'PROCESSING',
                             claim_token = ?,
                             lease_until = DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 120 SECOND),
                             attempt_count = attempt_count + 1,
                             updated_at = UTC_TIMESTAMP(3)
                         WHERE id = ?
                           AND status IN ('PENDING', 'FAILED_RETRYABLE', 'PROCESSING')
                           AND next_attempt_at <= UTC_TIMESTAMP(3)
                           AND (lease_until IS NULL OR lease_until < UTC_TIMESTAMP(3))
                         """)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setLong(2, id);
                int n = ps.executeUpdate();
                if (n == 1) {
                    winners.incrementAndGet();
                }
                return n;
            }
        }
    }
}
