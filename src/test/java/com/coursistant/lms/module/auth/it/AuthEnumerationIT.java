package com.coursistant.lms.module.auth.it;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"auth-it", "auth-phase3-local"})
class AuthEnumerationIT extends AuthIntegrationTestBase {

    @Test
    void loginFailureModes_shareStatusAndErrorType() throws Exception {
        String email = dataFactory.uniqueEmail("enum3");
        dataFactory.createStudent(1, email);

        MvcResult missing = performLogin("nobody-missing@ex.com", "WrongPass1!", "USER");
        MvcResult badPw = performLogin(email, "WrongPass1!", "USER");

        for (int i = 0; i < 5; i++) {
            performLogin(email, "WrongPass1!", "USER");
        }
        MvcResult locked = performLogin(email, AuthTestDataFactory.PASSWORD_PLAIN, "USER");

        String disabledEmail = dataFactory.uniqueEmail("enum-dis");
        var disabled = dataFactory.createStudent(1, disabledEmail);
        jdbcTemplate.update("UPDATE `user` SET status = ? WHERE id = ?", AccountStatus.DISABLED.name(), disabled.getId());
        MvcResult disabledLogin = performLogin(disabledEmail, AuthTestDataFactory.PASSWORD_PLAIN, "USER");

        String tenantEmail = dataFactory.uniqueEmail("enum-ten");
        dataFactory.createStudent(3, tenantEmail); // tenant 3 seeded DISABLED
        MvcResult tenantDisabled = performLogin(tenantEmail, AuthTestDataFactory.PASSWORD_PLAIN, "USER");

        for (MvcResult r : new MvcResult[]{missing, badPw, locked, disabledLogin, tenantDisabled}) {
            assertEquals(401, r.getResponse().getStatus());
            assertTrue(r.getResponse().getContentAsString().contains(ErrorType.INVALID_CREDENTIALS.name()));
            assertFalse(r.getResponse().getContentAsString().toLowerCase().contains("remaining"));
            assertFalse(r.getResponse().getContentAsString().toLowerCase().contains("locked"));
            assertFalse(r.getResponse().getContentAsString().toLowerCase().contains("disabled"));
        }
    }

    @Test
    void registerCode_existingVsMissing_samePublicOk() throws Exception {
        String existing = dataFactory.uniqueEmail("enum-reg-ex");
        dataFactory.createStudent(1, existing);
        String missing = dataFactory.uniqueEmail("enum-reg-miss");
        fakeEmailCapture.clear();

        MvcResult a = mockMvc.perform(post("/v1/auth/email-verifications/register").param("email", existing))
                .andExpect(status().isOk()).andReturn();
        int mailAfterExisting = fakeEmailCapture.size();
        MvcResult b = mockMvc.perform(post("/v1/auth/email-verifications/register").param("email", missing))
                .andExpect(status().isOk()).andReturn();

        assertEquals(a.getResponse().getStatus(), b.getResponse().getStatus());
        assertEquals(0, mailAfterExisting); // existing: silent, no mail
        assertEquals(1, fakeEmailCapture.size()); // missing: one mail
    }

    @Test
    void resetCode_existingVsMissing_samePublicOk() throws Exception {
        String existing = dataFactory.uniqueEmail("enum-rst-ex");
        dataFactory.createStudent(1, existing);
        String missing = dataFactory.uniqueEmail("enum-rst-miss");
        fakeEmailCapture.clear();

        MvcResult a = mockMvc.perform(post("/v1/auth/email-verifications/reset").param("email", existing))
                .andExpect(status().isOk()).andReturn();
        int mailAfterExisting = fakeEmailCapture.size();
        MvcResult b = mockMvc.perform(post("/v1/auth/email-verifications/reset").param("email", missing))
                .andExpect(status().isOk()).andReturn();

        assertEquals(a.getResponse().getStatus(), b.getResponse().getStatus());
        assertEquals(1, mailAfterExisting); // existing account gets reset mail
        assertEquals(1, fakeEmailCapture.size()); // missing: silent, no additional mail
    }

    private MvcResult performLogin(String email, String password, String role) throws Exception {
        return mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password
                                + "\",\"role\":\"" + role + "\"}"))
                .andReturn();
    }
}
