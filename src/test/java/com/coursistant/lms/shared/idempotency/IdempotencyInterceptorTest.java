package com.coursistant.lms.shared.idempotency;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {

    @Mock private StringRedisTemplate idempotencyRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        var field = IdempotencyInterceptor.class.getDeclaredField("idempotencyRedisTemplate");
        field.setAccessible(true);
        field.set(interceptor, idempotencyRedisTemplate);
        lenient().when(idempotencyRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void redisUnavailable_beforeWrite_returns503() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenThrow(new RuntimeException("down"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v2/courses");
        request.addHeader("Idempotency-Key", "abc");
        request.setAttribute("userId", 1);
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = handlerMethod();

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request, response, handler));
        assertEquals(ErrorType.IDEMPOTENCY_STORE_UNAVAILABLE, ex.getErrorType());
        assertEquals(503, ex.getErrorType().getHttpStatus().value());
    }

    @Test
    void keyMismatch_returns409() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);
        String otherFp = "deadbeef";
        when(valueOperations.get(anyString())).thenReturn(IdempotencyRecord.done(otherFp, 200, "{\"ok\":true}".getBytes()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v2/courses");
        request.addHeader("Idempotency-Key", "abc");
        request.setAttribute("userId", 1);
        request.setContent("{}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request, response, handlerMethod()));
        assertEquals(ErrorType.IDEMPOTENCY_KEY_MISMATCH, ex.getErrorType());
        assertEquals(409, ex.getErrorType().getHttpStatus().value());
    }

    @Test
    void missingKey_required() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v2/courses");
        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod()));
        assertEquals(ErrorType.IDEMPOTENCY_KEY_REQUIRED, ex.getErrorType());
        verifyNoInteractions(valueOperations);
    }

    private HandlerMethod handlerMethod() throws Exception {
        Method m = Sample.class.getMethod("create");
        return new HandlerMethod(new Sample(), m);
    }

    static class Sample {
        @Idempotent
        public void create() {
        }
    }
}
