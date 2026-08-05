package com.coursistant.lms.module.auth.it;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"auth-it", "auth-phase3-local"})
class AuthSensitiveLoggingIT extends AuthIntegrationTestBase {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attach() {
        logger = (Logger) LoggerFactory.getLogger("com.coursistant.lms.module.auth");
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        if (logger != null && appender != null) {
            logger.detachAppender(appender);
        }
    }

    @Test
    void httpFlows_doNotLeakSecretsInLogsOrResponse() throws Exception {
        String email = dataFactory.uniqueEmail("sens");
        dataFactory.createStudent(1, email);
        String password = AuthTestDataFactory.PASSWORD_PLAIN;

        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = login.getResponse().getContentAsString();
        assertFalse(body.contains("refreshToken"));
        assertFalse(body.contains(password));

        String access = com.jayway.jsonpath.JsonPath.read(body, "$.data.accessToken");
        mockMvc.perform(put("/v1/auth/password")
                        .header("Authorization", "Bearer " + access)
                        .header("Idempotency-Key", "sens-pw-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + password + "\",\"newPassword\":\"NewPassw0rd!\"}"))
                .andExpect(status().isOk());

        String joined = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertFalse(joined.contains(password));
        assertFalse(joined.contains("NewPassw0rd!"));
        assertFalse(joined.contains("Bearer " + access));
        assertFalse(joined.toLowerCase().contains("authorization=bearer"));
        assertFalse(joined.contains("BEGIN RSA PRIVATE KEY"));
    }
}
