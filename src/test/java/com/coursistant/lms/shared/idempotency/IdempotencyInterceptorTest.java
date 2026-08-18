package com.coursistant.lms.shared.idempotency;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.time.Duration;

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

    @Test
    void idemP1_emptyGetRetriesSetIfAbsent_doesNotUsePlainSet() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false, true);
        when(valueOperations.get(anyString())).thenReturn(null);

        MockHttpServletRequest request = request();
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod()));
        verify(valueOperations, times(2)).setIfAbsent(anyString(), anyString(), any());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        assertNotNull(request.getAttribute("idem.redisKey"));
    }

    @Test
    void idemP2_bothClaimsFailAndGetEmpty_is409InProgress() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod()));
        assertEquals(ErrorType.IDEMPOTENCY_REQUEST_IN_PROGRESS, ex.getErrorType());
        assertEquals(409, ex.getErrorType().getHttpStatus().value());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void idemP3_pendingRecord_is409InProgress() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(IdempotencyRecord.pending("fp"));

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod()));
        assertEquals(ErrorType.IDEMPOTENCY_REQUEST_IN_PROGRESS, ex.getErrorType());
    }

    @Test
    void idemP4_claimUsesConfiguredPendingTtl() throws Exception {
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        when(valueOperations.setIfAbsent(anyString(), anyString(), ttl.capture())).thenReturn(true);

        assertTrue(interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod()));
        assertEquals(Duration.ofSeconds(900), ttl.getValue());
    }

    @Test
    void idemP5_illegalState_is409InProgress() throws Exception {
        assertParseFailureYieldsInProgress("WUT:fingerprint::");
    }

    @Test
    void idemP5_illegalStatusNumber_is409InProgress() throws Exception {
        assertParseFailureYieldsInProgress("DONE:fingerprint:not-a-number:");
    }

    @Test
    void idemP5_illegalBase64_is409InProgress() throws Exception {
        assertParseFailureYieldsInProgress("DONE:fingerprint:200:%%%");
    }

    private void assertParseFailureYieldsInProgress(String redisValue) throws Exception {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn(redisValue);
        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request(), new MockHttpServletResponse(), handlerMethod()));
        assertEquals(ErrorType.IDEMPOTENCY_REQUEST_IN_PROGRESS, ex.getErrorType());
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v2/courses");
        request.addHeader("Idempotency-Key", "abc");
        request.setAttribute("userId", 1);
        return request;
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
