package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"auth-it", "auth-phase3-local"})
class AuthLocalPerformanceIT extends AuthIntegrationTestBase {

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    void loginRefreshAndCacheBaselines_noUnexpected500() throws Exception {
        List<Long> loginMs = new ArrayList<>();
        List<Long> refreshMs = new ArrayList<>();
        List<Long> cacheHitMs = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            String email = dataFactory.uniqueEmail("perf-login-" + i);
            dataFactory.createStudent(1, email);
            long t0 = System.nanoTime();
            MvcResult login = mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\""
                                    + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                    .andReturn();
            loginMs.add((System.nanoTime() - t0) / 1_000_000);
            assertEquals(200, login.getResponse().getStatus(), login.getResponse().getContentAsString());
            assertFalse(login.getResponse().getContentAsString().contains("\"code\":\"500\""));
        }

        String email = dataFactory.uniqueEmail("perf-ref");
        var user = dataFactory.createStudent(1, email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk()).andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        Cookie refresh = login.getResponse().getCookie("refreshToken");

        for (int i = 0; i < 50; i++) {
            if (i % 10 == 0) {
                wipeAuthRedisKeys(); // avoid rate-limit 429 distorting baseline hard gate
            }
            long t0 = System.nanoTime();
            MvcResult r = mockMvc.perform(post("/v1/auth/refresh-token").cookie(refresh)).andReturn();
            refreshMs.add((System.nanoTime() - t0) / 1_000_000);
            int st = r.getResponse().getStatus();
            assertTrue(st == 200 || st == 401 || st == 429,
                    "status=" + st);
            assertNotEquals(500, st);
            assertFalse(st == 200 && r.getResponse().getContentAsString().contains("REFRESH_TOKEN_REUSED"));
            Cookie next = r.getResponse().getCookie("refreshToken");
            if (next != null) {
                refresh = next;
            }
        }

        for (int i = 0; i < 200; i++) {
            long t0 = System.nanoTime();
            int status = mockMvc.perform(get("/v2/users/" + user.getId())
                            .header("Authorization", "Bearer " + access))
                    .andReturn().getResponse().getStatus();
            cacheHitMs.add((System.nanoTime() - t0) / 1_000_000);
            assertTrue(status == 200 || status == 401 || status == 403, "status=" + status);
            assertNotEquals(500, status);
        }

        // Multi-device refresh (clear IP refresh rate-limit window first; prod limit is 10/min)
        wipeAuthRedisKeys();
        List<Cookie> devices = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MvcResult d = mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\""
                                    + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                    .andExpect(status().isOk()).andReturn();
            devices.add(d.getResponse().getCookie("refreshToken"));
        }
        for (Cookie c : devices) {
            int st = mockMvc.perform(post("/v1/auth/refresh-token").cookie(c))
                    .andReturn().getResponse().getStatus();
            assertTrue(st == 200 || st == 429, "status=" + st);
            assertNotEquals(500, st);
        }
        assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).size() >= 5);

        System.out.printf("AuthLocalPerformanceIT login p50=%d p95=%d p99=%d | refresh p50=%d p95=%d | cache p50=%d p95=%d%n",
                percentile(loginMs, 50), percentile(loginMs, 95), percentile(loginMs, 99),
                percentile(refreshMs, 50), percentile(refreshMs, 95),
                percentile(cacheHitMs, 50), percentile(cacheHitMs, 95));
    }

    @Test
    void concurrentWrongLogin_countsWithout500() throws Exception {
        String email = dataFactory.uniqueEmail("perf-lock");
        dataFactory.createStudent(1, email);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger unexpected500 = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 3; j++) {
                        int status = mockMvc.perform(post("/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"email\":\"" + email + "\",\"password\":\"WrongPass1!\",\"role\":\"USER\"}"))
                                .andReturn().getResponse().getStatus();
                        if (status == 500) {
                            unexpected500.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    unexpected500.incrementAndGet();
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdownNow();
        assertEquals(0, unexpected500.get());
    }

    private static long percentile(List<Long> values, int pct) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int idx = Math.min(sorted.size() - 1, Math.max(0, (int) Math.ceil(pct / 100.0 * sorted.size()) - 1));
        return sorted.get(idx);
    }
}
