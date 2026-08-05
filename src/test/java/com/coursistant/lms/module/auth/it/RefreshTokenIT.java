package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RefreshTokenIT extends AuthIntegrationTestBase {

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    void refresh_rotatesCookie_andJsonHasOnlyAccess() throws Exception {
        String email = dataFactory.uniqueEmail("ref");
        dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");
        assertNotNull(refresh);

        MvcResult refreshed = mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isString())
                .andReturn();
        assertFalse(refreshed.getResponse().getContentAsString().contains(refresh.getValue()));
        Cookie next = refreshed.getResponse().getCookie("refreshToken");
        assertNotNull(next);
        assertNotEquals(refresh.getValue(), next.getValue());
    }

    @Test
    void refresh_missingCookie_401() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void refreshToken_cannotBeUsedAsBearer() throws Exception {
        String email = dataFactory.uniqueEmail("bearer");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");
        // Logout is cookie-based and may be permitAll; assert refresh UUID is not a valid Access Token.
        mockMvc.perform(get("/v2/users/" + user.getId()).header("Authorization", "Bearer " + refresh.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesDevice_idempotent() throws Exception {
        String email = dataFactory.uniqueEmail("logout");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");
        mockMvc.perform(post("/v1/auth/logout").cookie(refresh)).andExpect(status().isOk());
        assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).isEmpty());
        mockMvc.perform(post("/v1/auth/logout").cookie(refresh)).andExpect(status().isOk());
    }
}
