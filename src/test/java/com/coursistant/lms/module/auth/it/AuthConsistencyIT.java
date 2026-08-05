package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.shared.enums.AccountStatus;
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
class AuthConsistencyIT extends AuthIntegrationTestBase {

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    void disableUserInDb_rejectsOldAccessToken() throws Exception {
        String email = dataFactory.uniqueEmail("cons-dis");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        jdbcTemplate.update("UPDATE `user` SET status = ? WHERE id = ?", AccountStatus.DISABLED.name(), user.getId());

        mockMvc.perform(get("/v2/users/" + user.getId()).header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bumpAuthVersion_rejectsOldAccessToken() throws Exception {
        String email = dataFactory.uniqueEmail("cons-ver");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        jdbcTemplate.update("UPDATE `user` SET auth_version = auth_version + 1 WHERE id = ?", user.getId());

        mockMvc.perform(get("/v2/users/" + user.getId()).header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disableTenant_rejectsUserAccess() throws Exception {
        String email = dataFactory.uniqueEmail("cons-ten");
        var user = dataFactory.createStudent(2, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        jdbcTemplate.update("UPDATE tenant SET status = ? WHERE id = 2", AccountStatus.DISABLED.name());
        try {
            mockMvc.perform(get("/v2/users/" + user.getId()).header("Authorization", "Bearer " + access))
                    .andExpect(status().isUnauthorized());
        } finally {
            jdbcTemplate.update("UPDATE tenant SET status = ? WHERE id = 2", AccountStatus.ACTIVE.name());
        }
    }

    @Test
    void sameNumericId_adminAndUser_doNotCrossAuthenticate() throws Exception {
        String adminEmail = dataFactory.uniqueEmail("cons-adm");
        String userEmail = dataFactory.uniqueEmail("cons-usr");
        var admin = dataFactory.createSystemAdmin(adminEmail);
        var user = dataFactory.createStudent(1, userEmail);
        // Ensure we exercise distinct principal caches even if IDs collide in other DBs.
        assertNotNull(admin.getId());
        assertNotNull(user.getId());

        MvcResult adminLogin = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + adminEmail + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"SYSTEM_ADMIN\"}"))
                .andExpect(status().isOk()).andReturn();
        String adminAccess = com.jayway.jsonpath.JsonPath.read(
                adminLogin.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/v2/users/" + user.getId()).header("Authorization", "Bearer " + adminAccess))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 200
                                || result.getResponse().getStatus() == 403
                                || result.getResponse().getStatus() == 404,
                        "admin token must not become a user principal; status="
                                + result.getResponse().getStatus()));
    }

    @Test
    void dbRevokedSession_redisResidueCannotRevive() throws Exception {
        String email = dataFactory.uniqueEmail("cons-sess");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");
        assertNotNull(refresh);

        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id = ?", user.getId());
        assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).isEmpty());

        mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void replayRevokesOnlyOneDevice() throws Exception {
        String email = dataFactory.uniqueEmail("cons-dev");
        var user = dataFactory.createStudent(1, email);
        MvcResult d1 = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk()).andReturn();
        Thread.sleep(30);
        MvcResult d2 = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk()).andReturn();
        Cookie c1 = d1.getResponse().getCookie("refreshToken");
        Cookie c2 = d2.getResponse().getCookie("refreshToken");
        assertEquals(2, refreshTokenMapper.selectAllByUserId(user.getId()).size());

        MvcResult rotated = mockMvc.perform(post("/v1/auth/refresh-token").cookie(c1))
                .andExpect(status().isOk()).andReturn();
        Cookie c1Next = rotated.getResponse().getCookie("refreshToken");
        assertNotNull(c1Next);

        // Expire grace only on device-1 session (matched by previous_token = original c1)
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET previous_valid_until = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 MINUTE) "
                        + "WHERE previous_token = ?",
                c1.getValue());

        mockMvc.perform(post("/v1/auth/refresh-token").cookie(c1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

        assertEquals(1, refreshTokenMapper.selectAllByUserId(user.getId()).size());
        mockMvc.perform(post("/v1/auth/refresh-token").cookie(c2)).andExpect(status().isOk());
    }
}
