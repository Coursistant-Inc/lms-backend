package com.coursistant.lms.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer status;
    private String code;
    private T data;
    private String message;
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
