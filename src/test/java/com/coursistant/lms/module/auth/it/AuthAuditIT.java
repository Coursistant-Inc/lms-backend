package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"auth-it", "auth-phase3-local"})
class AuthAuditIT extends AuthIntegrationTestBase {

    @Test
    void passwordChange_writesSuccessAudit_withoutSecrets() throws Exception {
        String email = dataFactory.uniqueEmail("audit-pw");
        dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(put("/v1/auth/password")
                        .header("Authorization", "Bearer " + access)
                        .header("Idempotency-Key", "audit-pw-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"newPassword\":\"AuditPass1!\"}"))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_audit WHERE action = 'CHANGE_PASSWORD' AND result = 'SUCCESS'",
                Integer.class);
        assertNotNull(count);
        assertTrue(count >= 1);

        String row = jdbcTemplate.query(
                "SELECT CONCAT(IFNULL(action,''),'|',IFNULL(result,''),'|',IFNULL(before_json,''),'|',IFNULL(after_json,'')) "
                        + "FROM identity_audit WHERE action='CHANGE_PASSWORD' ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : "");
        assertFalse(row.contains("AuditPass1!"));
        assertFalse(row.contains(AuthTestDataFactory.PASSWORD_PLAIN));
        assertFalse(row.contains("Bearer "));
    }

    @Test
    void managedUserCreate_writesAudit() throws Exception {
        String sys = dataFactory.uniqueEmail("audit-sys");
        dataFactory.createSystemAdmin(sys);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + sys + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"SYSTEM_ADMIN\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(post("/v2/system/managed-users")
                        .header("Authorization", "Bearer " + access)
                        .header("Idempotency-Key", "audit-mu-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + dataFactory.uniqueEmail("audit-created")
                                + "\",\"name\":\"Audited\",\"role\":\"USER\",\"level\":\"STUDENT\",\"tenantId\":1}"))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_audit WHERE action LIKE '%CREATE%' AND result='SUCCESS'",
                Integer.class);
        assertNotNull(count);
        assertTrue(count >= 1);
    }

    @Test
    void refreshReplay_doesNotLeaveSuccessPasswordAudit() throws Exception {
        String email = dataFactory.uniqueEmail("audit-rep");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie first = login.getResponse().getCookie("refreshToken");
        mockMvc.perform(post("/v1/auth/refresh-token").cookie(first)).andExpect(status().isOk());
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET previous_valid_until = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 MINUTE) WHERE user_id = ?",
                user.getId());
        mockMvc.perform(post("/v1/auth/refresh-token").cookie(first)).andExpect(status().isUnauthorized());
        // Session revoke path must not invent CHANGE_PASSWORD success rows for this user flow
        Integer bad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_audit WHERE action='CHANGE_PASSWORD' AND target_id=?",
                Integer.class, user.getId());
        assertEquals(0, bad);
    }
}
