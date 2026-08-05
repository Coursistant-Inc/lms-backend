package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthContainerFaults;
import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles({"auth-it", "auth-phase3-local"})
class AuthRedisFailureIT extends AuthIntegrationTestBase {

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @AfterEach
    void alwaysUnpause() {
        AuthContainerFaults.unpauseRedis();
        AuthContainerFaults.unpauseMysql();
    }

    @Test
    void login_whenRedisPaused_returns503_andDoesNotCreateSession() throws Exception {
        String email = dataFactory.uniqueEmail("rfail-login");
        var user = dataFactory.createStudent(1, email);

        try {
            AuthContainerFaults.pauseRedis();
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\""
                                    + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("AUTH_SERVICE_TEMPORARILY_UNAVAILABLE"));
        } finally {
            AuthContainerFaults.unpauseRedis();
        }
        assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).isEmpty());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM identity_audit WHERE action LIKE '%LOGIN%' AND result='SUCCESS'", Integer.class));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString());
    }

    @Test
    void registerCode_whenRedisPaused_returns503_andSendsNoMail() throws Exception {
        String email = dataFactory.uniqueEmail("rfail-code");
        try {
            AuthContainerFaults.pauseRedis();
            mockMvc.perform(post("/v1/auth/email-verifications/register").param("email", email))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("AUTH_SERVICE_TEMPORARILY_UNAVAILABLE"));
            assertEquals(0, fakeEmailCapture.size());
        } finally {
            AuthContainerFaults.unpauseRedis();
        }
    }

    @Test
    void refresh_whenRedisPaused_returns503_notReused_andKeepsSession() throws Exception {
        String email = dataFactory.uniqueEmail("rfail-ref");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");
        assertNotNull(refresh);

        MvcResult failed;
        try {
            AuthContainerFaults.pauseRedis();
            failed = mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("AUTH_SERVICE_TEMPORARILY_UNAVAILABLE"))
                    .andReturn();
        } finally {
            AuthContainerFaults.unpauseRedis();
        }
        String setCookie = failed.getResponse().getHeader("Set-Cookie");
        assertTrue(setCookie == null || !setCookie.toLowerCase().contains("max-age=0"));
        assertEquals(1, refreshTokenMapper.selectAllByUserId(user.getId()).size());

        mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh))
                .andExpect(status().isOk());
    }

    @Test
    void logout_whenRedisPaused_stillRevokesDbSession() throws Exception {
        String email = dataFactory.uniqueEmail("rfail-out");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");

        try {
            AuthContainerFaults.pauseRedis();
            mockMvc.perform(post("/v1/auth/logout").cookie(refresh)).andExpect(status().isOk());
            assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).isEmpty());
        } finally {
            AuthContainerFaults.unpauseRedis();
        }
        mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/auth/logout").cookie(refresh)).andExpect(status().isOk());
    }

    @Test
    void protectedApi_whenRedisPaused_fallsBackToDb_orRejectsSafely() throws Exception {
        String email = dataFactory.uniqueEmail("rfail-prin");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        int status;
        try {
            AuthContainerFaults.pauseRedis();
            status = mockMvc.perform(get("/v2/users/" + user.getId())
                            .header("Authorization", "Bearer " + access))
                    .andReturn().getResponse().getStatus();
        } finally {
            AuthContainerFaults.unpauseRedis();
        }
        assertTrue(status == 200 || status == 401 || status == 403 || status == 503,
                "unexpected status " + status);
    }
}
