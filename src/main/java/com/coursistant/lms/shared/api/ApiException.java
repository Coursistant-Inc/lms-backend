package com.coursistant.lms.shared.api;

public class ApiException extends RuntimeException {

    private final ErrorType errorType;
    private final String customMessage;
    private final Object data;

    public ApiException(ErrorType errorType) {
        super(errorType.getDefaultMessage());
        this.errorType = errorType;
        this.customMessage = null;
        this.data = null;
    }

    public ApiException(ErrorType errorType, String customMessage) {
        super(customMessage);
        this.errorType = errorType;
        this.customMessage = customMessage;
        this.data = null;
    }

    public ApiException(ErrorType errorType, String customMessage, Object data) {
        super(customMessage != null ? customMessage : errorType.getDefaultMessage());
        this.errorType = errorType;
        this.customMessage = customMessage;
        this.data = data;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public Object getData() {
        return data;
    }

    public String getDisplayMessage() {
        return customMessage != null ? customMessage : errorType.getDefaultMessage();
    }
}
