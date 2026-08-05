package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.user.account.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIdentityAuthorizationIT extends AuthIntegrationTestBase {

    private String login(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    @Test
    void unauthenticated_protected_returns401() throws Exception {
        mockMvc.perform(get("/v2/admins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void user_cannotAccessSystemManagedUsers() throws Exception {
        String email = dataFactory.uniqueEmail("authz-u");
        dataFactory.createStudent(1, email);
        String token = login(email, "USER");
        mockMvc.perform(post("/v2/system/managed-users")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "authz-u-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@ex.com\",\"name\":\"X\",\"role\":\"USER\",\"level\":\"STUDENT\",\"tenantId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdmin_crossTenant_notFound() throws Exception {
        String email = dataFactory.uniqueEmail("authz-ta");
        dataFactory.createTenantAdmin(1, email);
        String token = login(email, "TENANT_ADMIN");
        User other = dataFactory.createStudent(2, dataFactory.uniqueEmail("other-t2"));
        mockMvc.perform(put("/v2/tenant/managed-users/" + other.getId() + "/role")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "authz-x-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\",\"level\":\"STUDENT\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantAdmin_cannotCreateSystemAdmin() throws Exception {
        String email = dataFactory.uniqueEmail("authz-ta2");
        dataFactory.createTenantAdmin(1, email);
        String token = login(email, "TENANT_ADMIN");
        mockMvc.perform(post("/v2/tenant/managed-users")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "authz-ta2-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + dataFactory.uniqueEmail("bad")
                                + "\",\"name\":\"X\",\"role\":\"SYSTEM_ADMIN\",\"level\":\"NOT_APPLICABLE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void systemAdmin_canCreateAcrossTenants() throws Exception {
        String email = dataFactory.uniqueEmail("authz-sys");
        dataFactory.createSystemAdmin(email);
        String token = login(email, "SYSTEM_ADMIN");
        mockMvc.perform(post("/v2/system/managed-users")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "it-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + dataFactory.uniqueEmail("sys-create")
                                + "\",\"name\":\"Created\",\"role\":\"USER\",\"level\":\"STUDENT\",\"tenantId\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    void getLogin_notPublic_whitelist() throws Exception {
        mockMvc.perform(get("/v1/auth/login")).andExpect(status().isUnauthorized());
    }
}
