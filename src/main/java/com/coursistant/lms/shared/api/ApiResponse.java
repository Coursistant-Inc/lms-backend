package com.coursistant.lms.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiResponse", description = "Unified JSON envelope for LMS APIs")
public class ApiResponse<T> {

    @Schema(description = "HTTP status mirrored in body", example = "200")
    private Integer status;
    @Schema(description = "Stable machine code (ErrorType.name() or SUCCESS)", example = "SUCCESS")
    private String code;
    @Schema(description = "Payload; omitted or null on many errors")
    private T data;
    @Schema(description = "Human-readable message", example = "Success")
    private String message;
    @Schema(description = "ISO-8601 timestamp", format = "date-time", example = "2026-07-23T10:00:00Z")
    private String timestamp;

    public ApiResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ApiResponse(Integer status, String code, T data, String message) {
        this.status = status;
        this.code = code;
        this.data = data;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "SUCCESS", data, "Success");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, "SUCCESS", data, message);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "SUCCESS", null, "Success");
    }

    public static <T> ApiResponse<T> error(ErrorType errorType) {
        return new ApiResponse<>(errorType.getStatusCode(), errorType.name(), null, errorType.getDefaultMessage());
    }

    public static <T> ApiResponse<T> error(ErrorType errorType, String customMessage) {
        return new ApiResponse<>(errorType.getStatusCode(), errorType.name(), null, customMessage);
    }

    public static <T> ApiResponse<T> error(ErrorType errorType, String customMessage, T data) {
        return new ApiResponse<>(errorType.getStatusCode(), errorType.name(), data, customMessage);
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
