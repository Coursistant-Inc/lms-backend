package com.coursistant.lms.module.course.enrollment.it;

import com.coursistant.lms.module.course.enrollment.it.support.TaIdentityIntegrationTestBase;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
import com.coursistant.lms.module.course.group.service.GroupMembershipService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolated Spring context for promote rollback: SpyBean only here (not on {@link TaIdentityFlowIT}).
 */
class TaIdentityPromoteRollbackIT extends TaIdentityIntegrationTestBase {

    @SpyBean
    private GroupMembershipService groupMembershipService;

    @AfterEach
    void resetSpy() {
        Mockito.reset(groupMembershipService);
    }

    @Test
    void promote_whenGroupEndThrows_rollsBackEnrollmentAndAudit() throws Exception {
        Fixture fx = newFixture();
        seedGroupMembership(fx.courseId(), fx.student().getId(), fx.instructor().getId());

        doThrow(new RuntimeException("forced group end failure"))
                .when(groupMembershipService)
                .endGroupMemberships(eq(fx.courseId()), eq(fx.student().getId()),
                        anyString(), any(), eq(GroupMembershipAudit.END_ON_TA_PROMOTION));

        mockMvc.perform(post("/v2/courses/{cid}/tas", fx.courseId())
                        .header("Authorization", "Bearer " + fx.instructorToken())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + fx.student().getId() + "}"))
                .andExpect(status().is5xxServerError());

        Map<String, Object> enr = jdbcTemplate.queryForMap(
                "SELECT course_role, active FROM enrollment WHERE course_id=? AND user_id=?",
                fx.courseId(), fx.student().getId());
        assertEquals("Student", enr.get("course_role"));
        assertTrue(isTruthy(enr.get("active")));

        Integer taAdded = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_audit_log WHERE course_id=? AND action='TA_ADDED'",
                Integer.class, fx.courseId());
        assertEquals(0, taAdded);

        Integer groupAudit = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_membership_audit WHERE course_id=? AND action=?",
                Integer.class, fx.courseId(), GroupMembershipAudit.END_ON_TA_PROMOTION);
        assertEquals(0, groupAudit);

        Integer memberships = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_membership WHERE course_id=? AND user_id=?",
                Integer.class, fx.courseId(), fx.student().getId());
        assertEquals(1, memberships);
    }

    private static boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return false;
    }
}
