package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.entity.RefreshToken;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RefreshTokenConcurrencyIT extends AuthIntegrationTestBase {

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    void concurrentRefresh_tenWays_singleRotation() throws Exception {
        concurrentRefresh(10);
    }

    @Test
    void concurrentRefresh_twoWays_singleRotation() throws Exception {
        concurrentRefresh(2);
    }

    @Test
    void concurrentRefresh_fiftyWays_singleRotation() throws Exception {
        concurrentRefresh(50);
    }

    private void concurrentRefresh(int n) throws Exception {
        String email = dataFactory.uniqueEmail("conc" + n);
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refresh = login.getResponse().getCookie("refreshToken");
        assertNotNull(refresh);
        String old = refresh.getValue();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        AtomicInteger ok = new AtomicInteger();
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                MvcResult r = mockMvc.perform(post("/v1/auth/refresh-token").cookie(new Cookie("refreshToken", old)))
                        .andReturn();
                if (r.getResponse().getStatus() == 200) {
                    ok.incrementAndGet();
                    Cookie c = r.getResponse().getCookie("refreshToken");
                    return c != null ? c.getValue() : null;
                }
                return null;
            }));
        }
        start.countDown();
        Set<String> newTokens = new java.util.HashSet<>();
        for (Future<String> f : futures) {
            String t = f.get();
            if (t != null) {
                newTokens.add(t);
            }
        }
        pool.shutdownNow();

        assertTrue(ok.get() >= 1);
        assertEquals(1, newTokens.size(), "all successful rotations must share one new refresh token");
        List<RefreshToken> sessions = refreshTokenMapper.selectAllByUserId(user.getId());
        assertEquals(1, sessions.size());
        assertEquals(newTokens.iterator().next(), sessions.get(0).getToken());
    }

    @Test
    void previousToken_outsideGrace_returnsReused_andRevokesOnlyThatSession() throws Exception {
        String email = dataFactory.uniqueEmail("replay");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie first = login.getResponse().getCookie("refreshToken");

        MvcResult rotated = mockMvc.perform(post("/v1/auth/refresh-token").cookie(first))
                .andExpect(status().isOk())
                .andReturn();
        Cookie current = rotated.getResponse().getCookie("refreshToken");

        // Force previous token outside grace without relying on Clock propagation into service beans.
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET previous_valid_until = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 MINUTE) WHERE user_id = ?",
                user.getId());

        mockMvc.perform(post("/v1/auth/refresh-token").cookie(first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

        assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).isEmpty());
        // current cookie also dead after revoke
        mockMvc.perform(post("/v1/auth/refresh-token").cookie(current))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void multiDevice_sixthEvictsOldest() throws Exception {
        String email = dataFactory.uniqueEmail("dev");
        var user = dataFactory.createStudent(1, email);
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            MvcResult login = mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\""
                                    + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                    .andExpect(status().isOk())
                    .andReturn();
            tokens.add(login.getResponse().getCookie("refreshToken").getValue());
            Thread.sleep(15); // ensure created_time ordering without long waits
        }
        assertEquals(5, refreshTokenMapper.selectAllByUserId(user.getId()).size());
    }
}
