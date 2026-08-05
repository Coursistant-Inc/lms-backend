package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.identity.repository.AccountIdentityMapper;
import com.coursistant.lms.module.auth.identity.service.AccountIdentityService;
import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthRegistrationIT extends AuthIntegrationTestBase {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AccountIdentityMapper accountIdentityMapper;

    @Test
    void register_happyPath_viaFakeEmailCode() throws Exception {
        String email = dataFactory.uniqueEmail("reg");
        mockMvc.perform(post("/v1/auth/email-verifications/register")
                        .param("email", email)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertEquals(1, fakeEmailCapture.size());
        String code = extractCode();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"name\":\"Reg User\",\"verificationCode\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        assertNotNull(userMapper.selectByEmail(email));
        assertNotNull(accountIdentityMapper.selectByEmail(AccountIdentityService.normalizeEmail(email)));
        assertEquals("USER", userMapper.selectByEmail(email).getRole());
        assertEquals("STUDENT", userMapper.selectByEmail(email).getLevel());
        assertEquals(1, userMapper.selectByEmail(email).getTenantId());
    }

    @Test
    void existingEmail_requestCode_silentSuccess_noMail() throws Exception {
        String email = dataFactory.uniqueEmail("exist");
        dataFactory.createStudent(1, email);
        fakeEmailCapture.clear();
        mockMvc.perform(post("/v1/auth/email-verifications/register")
                        .param("email", email)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertEquals(0, fakeEmailCapture.size());
    }

    @Test
    void wrongCode_rejected() throws Exception {
        String email = dataFactory.uniqueEmail("badcode");
        mockMvc.perform(post("/v1/auth/email-verifications/register")
                        .param("email", email)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN
                                + "\",\"name\":\"X\",\"verificationCode\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    private String extractCode() {
        return fakeEmailCapture.all().stream()
                .map(e -> {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(e.body());
                    return m.find() ? m.group(1) : null;
                })
                .filter(c -> c != null)
                .findFirst()
                .orElseThrow();
    }
}
