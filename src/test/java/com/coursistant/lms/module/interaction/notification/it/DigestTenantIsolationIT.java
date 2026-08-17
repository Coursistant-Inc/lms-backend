package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase1SpringITBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DigestTenantIsolationIT extends NotificationPhase1SpringITBase {

    private static final LocalDate DIGEST_DATE = LocalDate.of(2026, 8, 16);

    @Autowired
    private DailyDigestService dailyDigestService;

    @Test
    void run_scopedToTenant_doesNotClaimOtherTenantEnvelope() {
        int user1 = insertUser("digest-t1-" + uuid() + "@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        insertDelivery(uuid(), user1, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9, "ann-" + uuid(),
                "DAILY_DIGEST", "PENDING", courseId, DIGEST_DATE);

        jdbcTemplate.update("""
                INSERT INTO tenant (id, name, timezone, status)
                VALUES (2, 'p1-tenant-2', 'America/Los_Angeles', 'ACTIVE')
                """);
        int user2 = insertUserForTenant(2, "digest-t2-" + uuid() + "@example.com");
        long foreignDeliveryId = insertDelivery(uuid(), user2, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9,
                "ann-" + uuid(), "DAILY_DIGEST", "PENDING", courseId, DIGEST_DATE);
        jdbcTemplate.update("UPDATE notification_delivery SET tenant_id = 2 WHERE id = ?", foreignDeliveryId);

        jdbcTemplate.update("""
                INSERT INTO notification_digest_email (
                  tenant_id, recipient_user_id, digest_date, status, item_count, attempt_count,
                  next_attempt_at, unknown_outcome_count, created_at, updated_at
                ) VALUES (2, ?, ?, 'PENDING', 1, 0, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE), 0,
                  UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, user2, DIGEST_DATE);
        Long foreignDigestId = jdbcTemplate.queryForObject(
                "SELECT id FROM notification_digest_email WHERE recipient_user_id = ?", Long.class, user2);
        jdbcTemplate.update("""
                UPDATE notification_delivery
                SET digest_email_id = ?, status = 'PROCESSING'
                WHERE id = ?
                """, foreignDigestId, foreignDeliveryId);

        dailyDigestService.run(DIGEST_DATE, 1);

        String foreignStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, foreignDigestId);
        String foreignItem = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_delivery WHERE id = ?", String.class, foreignDeliveryId);
        assertEquals("PENDING", foreignStatus, "tenant 2 envelope must not be claimed by tenant 1 run");
        assertEquals("PROCESSING", foreignItem);
        assertEquals(1, fakeNotificationEmailSender.messages().size(),
                "only tenant 1 digest should be sent");
    }

    @Test
    void run_doesNotClaimFutureDigestDate() {
        int userId = insertUser("digest-future-" + uuid() + "@example.com", true, "ACTIVE");
        int instructorId = insertInstructor();
        int courseId = insertCourse(instructorId);
        LocalDate future = DIGEST_DATE.plusDays(1);
        insertDelivery(uuid(), userId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", 9, "ann-" + uuid(),
                "DAILY_DIGEST", "PENDING", courseId, future);

        jdbcTemplate.update("""
                INSERT INTO notification_digest_email (
                  tenant_id, recipient_user_id, digest_date, status, item_count, attempt_count,
                  next_attempt_at, unknown_outcome_count, created_at, updated_at
                ) VALUES (1, ?, ?, 'PENDING', 1, 0, DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE), 0,
                  UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, userId, future);
        Long digestId = jdbcTemplate.queryForObject(
                "SELECT id FROM notification_digest_email WHERE recipient_user_id = ?", Long.class, userId);
        jdbcTemplate.update("""
                UPDATE notification_delivery
                SET digest_email_id = ?, status = 'PROCESSING'
                WHERE recipient_user_id = ?
                """, digestId, userId);

        dailyDigestService.run(DIGEST_DATE, 1);

        String parent = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_digest_email WHERE id = ?", String.class, digestId);
        assertEquals("PENDING", parent, "future digest_date envelope must not be claimed");
        assertEquals(0, fakeNotificationEmailSender.messages().size());
        assertEquals(1, count("""
                SELECT COUNT(*) FROM notification_delivery
                WHERE recipient_user_id = ? AND status = 'PROCESSING' AND digest_date = ?
                """, userId, future));
    }

    private int insertUserForTenant(int tenantId, String email) {
        String username = "u-" + uuid();
        jdbcTemplate.update("""
                        INSERT INTO `user` (tenant_id, username, password, name, email, role, level, status,
                          email_notifications)
                        VALUES (?, ?, 'x', 'Test', ?, 'USER', 'STUDENT', 'ACTIVE', 1)
                        """,
                tenantId, username, email);
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM `user` WHERE email = ?", Integer.class, email);
        return id == null ? -1 : id;
    }
}
