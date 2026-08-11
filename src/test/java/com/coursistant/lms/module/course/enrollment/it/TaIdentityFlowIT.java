package com.coursistant.lms.module.course.enrollment.it;

import com.coursistant.lms.module.course.enrollment.it.support.TaIdentityIntegrationTestBase;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boot + MockMvc + Testcontainers MySQL/Redis TA identity flow.
 * Outer class does <b>not</b> Spy/Mock {@code GroupMembershipService}.
 */
class TaIdentityFlowIT extends TaIdentityIntegrationTestBase {

    @Test
    void assignmentSubmission_promoteFreezesAndExcludesFromRoster_keepsGrade() throws Exception {
        Fixture fx = newFixture();
        int assignmentId = seedPublishedAssignment(fx.courseId(), fx.instructor().getId(), fx.student().getId());

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fx.student().getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.courseRole").value("TA"))
                .andExpect(jsonPath("$.data.assignmentSubmitFrozen").value(true));

        Map<String, Object> enr = jdbcTemplate.queryForMap(
                "SELECT course_role, assignment_submit_frozen, active FROM enrollment "
                        + "WHERE course_id=? AND user_id=?",
                fx.courseId(), fx.student().getId());
        assertEquals("TA", enr.get("course_role"));
        assertTrue(isTruthy(enr.get("assignment_submit_frozen")));
        assertTrue(isTruthy(enr.get("active")));

        String level = jdbcTemplate.queryForObject(
                "SELECT level FROM `user` WHERE id=?", String.class, fx.student().getId());
        assertEquals("STUDENT", level);

        mockMvc.perform(get("/v2/courses/{cid}/assignments/{aid}/grading-roster",
                        fx.courseId(), assignmentId)
                        .header("Authorization", "Bearer " + fx.instructorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.studentUserId==" + fx.student().getId() + ")]").isEmpty());

        Integer gradeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assignment_grade WHERE assignment_id=? AND student_user_id=? AND status='Released'",
                Integer.class, assignmentId, fx.student().getId());
        assertEquals(1, gradeCount);

        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_audit_log WHERE course_id=? AND action='TA_ADDED' AND target_id=?",
                Integer.class, fx.courseId(), fx.student().getId());
        assertEquals(1, audits);
    }

    @Test
    void inProgressQuizAttempt_promoteFinalizes_andStartRejected() throws Exception {
        Fixture fx = newFixture();
        int quizId = seedInProgressQuizAttempt(fx.courseId(), fx.instructor().getId(), fx.student().getId());

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fx.student().getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseRole").value("TA"));

        Map<String, Object> attempt = jdbcTemplate.queryForMap(
                "SELECT status, close_reason FROM quiz_attempt WHERE quiz_id=? AND user_id=?",
                quizId, fx.student().getId());
        assertNotEquals("InProgress", attempt.get("status"));
        assertEquals("MEMBERSHIP_INELIGIBLE", attempt.get("close_reason"));

        String studentToken = loginUser(fx.student().getEmail());
        mockMvc.perform(post("/v2/courses/{cid}/quizzes/{qid}/attempts", fx.courseId(), quizId)
                        .header("Authorization", "Bearer " + studentToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorType.ACCESS_DENIED.name()));
    }

    @Test
    void groupMembership_promoteEndsWithTaPromotionReason() throws Exception {
        Fixture fx = newFixture();
        seedGroupMembership(fx.courseId(), fx.student().getId(), fx.instructor().getId());

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fx.student().getId() + "}"))
                .andExpect(status().isOk());

        Integer memberships = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_membership WHERE course_id=? AND user_id=?",
                Integer.class, fx.courseId(), fx.student().getId());
        assertEquals(0, memberships);

        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_membership_audit WHERE course_id=? AND target_user_id=? AND action=?",
                Integer.class, fx.courseId(), fx.student().getId(),
                GroupMembershipAudit.END_ON_TA_PROMOTION);
        assertEquals(1, audits);
    }

    @Test
    void promotePatchDelete_andIdempotency() throws Exception {
        Fixture fx = newFixture();
        String key = UUID.randomUUID().toString();

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fx.student().getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.IDEMPOTENCY_KEY_REQUIRED.name()));

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fx.student().getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseRole").value("TA"));

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fx.student().getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseRole").value("TA"));

        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_audit_log WHERE course_id=? AND action='TA_ADDED'",
                Integer.class, fx.courseId());
        assertEquals(1, audits);

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + (fx.student().getId() + 99999) + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorType.IDEMPOTENCY_KEY_MISMATCH.name()));

        mockMvc.perform(patch("/v2/courses/{cid}/tas/{uid}/permissions", fx.courseId(), fx.student().getId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canGrade\":true,\"canPostAnnouncements\":false,"
                                + "\"canManageGroups\":false,\"canManageCourseEvents\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canGrade").value(true));

        mockMvc.perform(delete("/v2/courses/{cid}/tas/{uid}", fx.courseId(), fx.student().getId())
                        .header("Authorization", "Bearer " + fx.instructorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseRole").value("Student"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.canGrade").value(false))
                .andExpect(jsonPath("$.data.assignmentSubmitFrozen").value(true));

        Integer removed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_audit_log WHERE course_id=? AND action='TA_REMOVED'",
                Integer.class, fx.courseId());
        assertEquals(1, removed);
    }

    @Test
    void concurrentPromote_andPromoteVsRevoke_singleLegalRow() throws Exception {
        Fixture fx = newFixture();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();

        Future<?> promoteA = pool.submit(() -> {
            await(start);
            try {
                mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                                .header("Authorization", "Bearer " + fx.instructorToken())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":" + fx.student().getId() + "}"))
                        .andExpect(status().isOk());
                ok.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Future<?> promoteB = pool.submit(() -> {
            await(start);
            try {
                mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                                .header("Authorization", "Bearer " + fx.instructorToken())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":" + fx.student().getId() + "}"))
                        .andExpect(status().isOk());
                ok.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        start.countDown();
        promoteA.get(30, TimeUnit.SECONDS);
        promoteB.get(30, TimeUnit.SECONDS);

        Map<String, Object> afterPromote = jdbcTemplate.queryForMap(
                "SELECT course_role, active FROM enrollment WHERE course_id=? AND user_id=?",
                fx.courseId(), fx.student().getId());
        assertEquals("TA", afterPromote.get("course_role"));
        assertTrue(isTruthy(afterPromote.get("active")));
        assertEquals(2, ok.get());

        CountDownLatch start2 = new CountDownLatch(1);
        Future<?> promoteAgain = pool.submit(() -> {
            await(start2);
            try {
                mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                                .header("Authorization", "Bearer " + fx.instructorToken())
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":" + fx.student().getId() + "}"))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Future<?> revoke = pool.submit(() -> {
            await(start2);
            try {
                mockMvc.perform(delete("/v2/courses/{cid}/tas/{uid}", fx.courseId(), fx.student().getId())
                                .header("Authorization", "Bearer " + fx.instructorToken()))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        start2.countDown();
        promoteAgain.get(30, TimeUnit.SECONDS);
        revoke.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        Map<String, Object> finalRow = jdbcTemplate.queryForMap(
                "SELECT course_role, active FROM enrollment WHERE course_id=? AND user_id=?",
                fx.courseId(), fx.student().getId());
        assertTrue(isTruthy(finalRow.get("active")));
        assertTrue(List.of("Student", "TA").contains(finalRow.get("course_role")));
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enrollment WHERE course_id=? AND user_id=?",
                Integer.class, fx.courseId(), fx.student().getId());
        assertEquals(1, rows);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
    }

    static boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return false;
    }
}
