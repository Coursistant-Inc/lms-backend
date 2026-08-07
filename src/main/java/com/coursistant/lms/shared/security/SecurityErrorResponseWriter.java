package com.coursistant.lms.shared.security;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes auth/security failures as the standard {@link ApiResponse} JSON envelope.
 */
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ApiException exception) throws IOException {
        ErrorType type = exception.getErrorType() == null ? ErrorType.UNAUTHORIZED : exception.getErrorType();
        String message = exception.getMessage() == null ? type.getDefaultMessage() : exception.getMessage();
        response.setStatus(type.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(type, message));
    }

    public void write(HttpServletResponse response, ErrorType type, String message) throws IOException {
        write(response, new ApiException(type, message == null ? type.getDefaultMessage() : message));
    }
}
