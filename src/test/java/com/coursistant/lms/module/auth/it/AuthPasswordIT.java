package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.auth.admin.repository.AdminMapper;
import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.token.repository.RefreshTokenMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthPasswordIT extends AuthIntegrationTestBase {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Test
    void changePassword_invalidatesOldAccess_andRevokesSessions() throws Exception {
        String email = dataFactory.uniqueEmail("chpw");
        User user = dataFactory.createStudent(1, email);

        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(put("/v1/auth/password")
                        .header("Authorization", "Bearer " + access)
                        .header("Idempotency-Key", "pw-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"newPassword\":\"NewPassw0rd!\"}"))
                .andExpect(status().isOk());

        User after = userMapper.selectById(user.getId());
        assertEquals(2, after.getAuthVersion());
        assertTrue(refreshTokenMapper.selectAllByUserId(user.getId()).isEmpty());

        mockMvc.perform(get("/v2/users/" + user.getId()).header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPassword_viaCode_forUser() throws Exception {
        String email = dataFactory.uniqueEmail("rst");
        dataFactory.createStudent(1, email);
        mockMvc.perform(post("/v1/auth/email-verifications/reset")
                        .param("email", email)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        String code = fakeEmailCapture.all().stream()
                .map(e -> {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(e.body());
                    return m.find() ? m.group(1) : null;
                }).filter(c -> c != null).findFirst().orElseThrow();

        mockMvc.perform(post("/v1/auth/password-resets")
                        .header("Idempotency-Key", "rst-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"verificationCode\":\"" + code
                                + "\",\"newPassword\":\"ResetPass1!\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"ResetPass1!\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void systemAdmin_changePassword() throws Exception {
        String email = dataFactory.uniqueEmail("admpw");
        Admin admin = dataFactory.createSystemAdmin(email);
        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"SYSTEM_ADMIN\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        mockMvc.perform(put("/v1/auth/password")
                        .header("Authorization", "Bearer " + access)
                        .header("Idempotency-Key", "admpw-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"newPassword\":\"AdminNew1!\"}"))
                .andExpect(status().isOk());
        assertEquals(2, adminMapper.selectById(admin.getId()).getAuthVersion());
    }
}
