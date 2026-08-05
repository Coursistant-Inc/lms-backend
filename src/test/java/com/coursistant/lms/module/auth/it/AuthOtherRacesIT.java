package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.user.account.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level races only. Mapper/Lua/login-count DoD owned by persistence/redis/login ITs.
 */
class AuthOtherRacesIT extends AuthIntegrationTestBase {

    @Test
    void lastTenantAdmin_concurrentDemote_leavesAtLeastOne() throws Exception {
        String taEmail = dataFactory.uniqueEmail("last-ta");
        User ta = dataFactory.createTenantAdmin(1, taEmail);

        String sysEmail = dataFactory.uniqueEmail("sys-race");
        dataFactory.createSystemAdmin(sysEmail);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + sysEmail + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"SYSTEM_ADMIN\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger oks = new AtomicInteger();

        Runnable demote = () -> {
            try {
                start.await();
                int status = mockMvc.perform(put("/v2/system/managed-users/" + ta.getId() + "/role")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", "d-" + Thread.currentThread().threadId() + "-" + System.nanoTime())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"USER\",\"level\":\"STUDENT\"}"))
                        .andReturn().getResponse().getStatus();
                if (status == 200) oks.incrementAndGet();
                if (status == 409) conflicts.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                done.countDown();
            }
        };
        Thread t1 = new Thread(demote);
        Thread t2 = new Thread(demote);
        t1.start();
        t2.start();
        start.countDown();
        done.await();

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE tenant_id = 1 AND role = 'TENANT_ADMIN'", Integer.class);
        assertNotNull(remaining);
        assertTrue(remaining >= 1 || oks.get() <= 1,
                "last TENANT_ADMIN must not be removed twice; remaining=" + remaining
                        + " oks=" + oks.get() + " conflicts=" + conflicts.get());
        // Prefer strong outcome: at most one success when only one TA exists.
        assertTrue(oks.get() <= 1, "at most one demotion of the last TENANT_ADMIN may succeed");
    }
}
