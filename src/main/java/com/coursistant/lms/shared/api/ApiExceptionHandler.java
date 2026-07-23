package com.coursistant.lms.shared.api;

import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.shared.web.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException e) {
        ErrorType errorType = e.getErrorType();
        log.warn("ApiException: {} - {}", errorType.name(), e.getDisplayMessage());
        ApiResponse<Object> body = ApiResponse.error(errorType, e.getDisplayMessage());
        return ResponseEntity.status(errorType.getHttpStatus()).body(body);
    }

    /**
     * Temporary: forward old-module CustomException as legacy Result (HTTP 200).
     * Remove this handler once all modules are migrated to ApiException.
     */
    @ExceptionHandler(CustomException.class)
    public Result handleCustomException(CustomException e) {
        return Result.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        ApiResponse<Object> body = ApiResponse.error(ErrorType.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(ErrorType.INTERNAL_SERVER_ERROR.getHttpStatus()).body(body);
    }
}
