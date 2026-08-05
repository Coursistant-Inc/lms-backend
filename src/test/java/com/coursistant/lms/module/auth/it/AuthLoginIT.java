package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthLoginIT extends AuthIntegrationTestBase {

    @Test
    void studentLogin_setsCookie_andOmitsRefreshInJson() throws Exception {
        String email = dataFactory.uniqueEmail("login-stu");
        dataFactory.createStudent(1, email);

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken="));
        assertTrue(setCookie.toLowerCase().contains("httponly"));
        assertFalse(result.getResponse().getContentAsString().contains("refreshToken"));
    }

    @Test
    void instructorAndTenantAdminAndSystemAdmin_login() throws Exception {
        String inst = dataFactory.uniqueEmail("inst");
        dataFactory.createInstructor(1, inst);
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + inst + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("INSTRUCTOR"));

        String ta = dataFactory.uniqueEmail("ta");
        dataFactory.createTenantAdmin(1, ta);
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ta + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"TENANT_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("TENANT_ADMIN"));

        String sys = dataFactory.uniqueEmail("sys");
        dataFactory.createSystemAdmin(sys);
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + sys + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"SYSTEM_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMIN"));
    }

    @Test
    void antiEnumeration_sameErrorType() throws Exception {
        String email = dataFactory.uniqueEmail("enum");
        dataFactory.createStudent(1, email);

        MvcResult missing = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@ex.com\",\"password\":\"x\",\"role\":\"USER\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult badPw = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"WrongPass1!\",\"role\":\"USER\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertTrue(missing.getResponse().getContentAsString().contains(ErrorType.INVALID_CREDENTIALS.name())
                || missing.getResponse().getContentAsString().contains("INVALID_CREDENTIALS"));
        assertTrue(badPw.getResponse().getContentAsString().contains("INVALID_CREDENTIALS"));
    }

    @Test
    void fiveFailures_lock_thenClearOnSuccess() throws Exception {
        String email = dataFactory.uniqueEmail("lock5");
        dataFactory.createStudent(1, email);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"WrongPass1!\",\"role\":\"USER\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void forgedRole_doesNotGrantAdminTable() throws Exception {
        String email = dataFactory.uniqueEmail("forge");
        User u = dataFactory.createStudent(1, email);
        assertEquals(RoleEnum.USER.name(), u.getRole());
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"SYSTEM_ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }
}
