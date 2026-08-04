package com.coursistant.lms.module.auth.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.coursistant.lms.module.auth.session.service.LoginGuardService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.shared.api.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSensitiveLoggingTest {

    @Mock
    private RedisTemplate<String, Object> generalRedisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LoginGuardService loginGuardService;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(LoginGuardService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        lenient().when(generalRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(generalRedisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    @Test
    void loginFailureLogs_doNotContainPasswordOrRemainingAttempts() {
        when(valueOperations.increment(anyString())).thenReturn(2L);
        try {
            loginGuardService.recordFailure(LoginGuardService.ACCOUNT_USER, "secret-user@example.com", "BAD_PASSWORD");
        } catch (ApiException ignored) {
        }

        String joined = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        // reason codes may include BAD_PASSWORD; assert no plaintext secret / remaining-attempts leak
        assertFalse(joined.contains("Passw0rd"));
        assertFalse(joined.contains("secret-user@example.com"));
        assertFalse(joined.toLowerCase().contains("remaining"));
        assertFalse(joined.matches("(?s).*\\bpassword\\s*=.*"));
        assertTrue(joined.contains("BAD_PASSWORD") || joined.contains("emailHash="));
    }

    @Test
    void userToString_doesNotExposePasswordHash() {
        User user = new User();
        user.setId(1);
        user.setEmail("u@example.com");
        user.setPassword("SUPER_SECRET_HASH");
        String s = user.toString();
        assertFalse(s.contains("SUPER_SECRET_HASH"));
        assertFalse(s.contains("Bearer "));
        assertFalse(s.contains("Authorization"));
        assertTrue(s.contains("\"password\":\"***\"") || !s.toLowerCase().contains("password"));
    }
}
