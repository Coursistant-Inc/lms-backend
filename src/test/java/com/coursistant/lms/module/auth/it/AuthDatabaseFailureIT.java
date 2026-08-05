package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.it.support.AuthContainerFaults;
import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.module.auth.token.service.RefreshTokenReusedException;
import com.coursistant.lms.module.auth.token.service.RefreshTokenService;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles({"auth-it", "auth-phase3-local"})
class AuthDatabaseFailureIT extends AuthIntegrationTestBase {

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AccountIdentityService accountIdentityService;

    @AfterEach
    void alwaysUnpause() {
        AuthContainerFaults.unpauseMysql();
        AuthContainerFaults.unpauseRedis();
    }

    @Test
    void login_whenMysqlPaused_returnsUniformUnauthorized_or503() throws Exception {
        String email = dataFactory.uniqueEmail("dbfail-login");
        dataFactory.createStudent(1, email);

        MvcResult result;
        try {
            AuthContainerFaults.pauseMysql();
            result = mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\""
                                    + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                    .andReturn();
        } finally {
            AuthContainerFaults.unpauseMysql();
        }
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        assertTrue(status == 401 || status == 503 || status == 500,
                "status=" + status + " body=" + body);
        if (status == 401) {
            assertTrue(body.contains("INVALID_CREDENTIALS"));
            assertFalse(body.toLowerCase().contains(email.toLowerCase()));
        }
        awaitMysqlReady();
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void logout_whenMysqlPaused_doesNotSucceedByRedisOnly() throws Exception {
        String email = dataFactory.uniqueEmail("dbfail-out");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");

        int status;
        try {
            AuthContainerFaults.pauseMysql();
            status = mockMvc.perform(post("/v1/auth/logout").cookie(refresh))
                    .andReturn().getResponse().getStatus();
        } finally {
            AuthContainerFaults.unpauseMysql();
        }
        assertTrue(status == 401 || status == 500 || status == 503,
                "logout must not report success when DB revoke fails; status=" + status);
        awaitMysqlReady();
        assertEquals(1, refreshTokenMapper.selectAllByUserId(user.getId()).size());
    }

    @Test
    void register_identityConflict_rollsBackUserRow_andRequiresNewCode() throws Exception {
        String email = dataFactory.uniqueEmail("dbfail-reg");
        mockMvc.perform(post("/v1/auth/email-verifications/register").param("email", email))
                .andExpect(status().isOk());
        String code = extractCode();

        // Occupy normalized email before register claimEmail runs (after user insert).
        accountIdentityService.claimEmail(email, AccountIdentityService.PRINCIPAL_ADMIN, 999_001);

        mockMvc.perform(post("/v1/auth/register")
                        .header("Idempotency-Key", "reg-conflict-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"name\":\"X\",\"verificationCode\":\"" + code + "\"}"))
                .andExpect(status().isConflict());

        assertNull(userMapper.selectByEmail(email));
        Integer sessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens", Integer.class);
        assertEquals(0, sessions);

        // Code already consumed → must request a new one
        mockMvc.perform(post("/v1/auth/register")
                        .header("Idempotency-Key", "reg-reuse-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"name\":\"X\",\"verificationCode\":\"" + code + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_wrongCode_leavesNoUser() throws Exception {
        String email = dataFactory.uniqueEmail("dbfail-badcode");
        mockMvc.perform(post("/v1/auth/email-verifications/register").param("email", email))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/auth/register")
                        .header("Idempotency-Key", "bad-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"name\":\"X\",\"verificationCode\":\"000000\"}"))
                .andExpect(status().isBadRequest());
        assertNull(jdbcTemplate.query(
                "SELECT id FROM `user` WHERE email = ?",
                rs -> rs.next() ? rs.getInt(1) : null,
                email));
    }

    @Test
    void changePassword_wrongCurrent_keepsVersionSessions_andNoSuccessAudit() throws Exception {
        String email = dataFactory.uniqueEmail("dbfail-pw");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        assertEquals(1, refreshTokenMapper.selectAllByUserId(user.getId()).size());

        mockMvc.perform(put("/v1/auth/password")
                        .header("Authorization", "Bearer " + access)
                        .header("Idempotency-Key", "pw-bad-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"WrongPass1!\",\"newPassword\":\"NewPassw0rd!\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(1, userMapper.selectById(user.getId()).getAuthVersion());
        assertEquals(1, refreshTokenMapper.selectAllByUserId(user.getId()).size());
        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_audit WHERE action='CHANGE_PASSWORD' AND result='SUCCESS' AND target_id=?",
                Integer.class, user.getId());
        assertEquals(0, audits);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refreshReplay_outsideGrace_commitsRevoke_viaDedicatedException() throws Exception {
        String email = dataFactory.uniqueEmail("dbfail-reuse");
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

        assertThrows(RefreshTokenReusedException.class, () -> refreshTokenService.getNewAccessToken(first.getValue()));
        assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).isEmpty());
    }

    @Test
    void halfRotationFailure_doesNotReportReused_andLeavesSafeState() throws Exception {
        String email = dataFactory.uniqueEmail("dbfail-half");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");
        assertNotNull(refresh);

        MvcResult during;
        try {
            AuthContainerFaults.pauseMysql();
            during = mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh)).andReturn();
        } finally {
            AuthContainerFaults.unpauseMysql();
        }
        assertTrue(during.getResponse().getStatus() == 401
                        || during.getResponse().getStatus() == 503
                        || during.getResponse().getStatus() == 500,
                "status=" + during.getResponse().getStatus());
        assertFalse(during.getResponse().getContentAsString().contains("REFRESH_TOKEN_REUSED"));
        awaitMysqlReady();

        MvcResult after = mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh)).andReturn();
        String body = after.getResponse().getContentAsString();
        assertFalse(body.contains("REFRESH_TOKEN_REUSED"), body);
        if (after.getResponse().getStatus() == 200) {
            assertEquals(1, refreshTokenMapper.selectAllByUserId(user.getId()).size());
        }
    }

    private void awaitMysqlReady() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                return;
            } catch (Exception e) {
                Thread.sleep(250);
            }
        }
        fail("MySQL did not recover after unpause");
    }

    private String extractCode() {
        return fakeEmailCapture.all().stream()
                .map(e -> {
                    Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(e.body());
                    return m.find() ? m.group(1) : null;
                })
                .filter(c -> c != null)
                .findFirst()
                .orElseThrow();
    }
}
