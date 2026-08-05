package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthCookieContractIT extends AuthIntegrationTestBase {

    @Test
    void loginCookie_httpOnlySecureSameSiteLaxPath() throws Exception {
        String email = dataFactory.uniqueEmail("cookie");
        dataFactory.createStudent(1, email);
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        String lower = setCookie.toLowerCase();
        assertTrue(lower.contains("httponly"));
        assertTrue(lower.contains("secure"));
        assertTrue(lower.contains("samesite=lax"));
        assertTrue(setCookie.contains("Path=/") || lower.contains("path=/"));
        assertFalse(result.getResponse().getContentAsString().contains("refreshToken"));
    }
}
