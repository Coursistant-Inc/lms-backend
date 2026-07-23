package com.coursistant.lms.shared.api;

public class ApiException extends RuntimeException {

    private final ErrorType errorType;
    private final String customMessage;

    public ApiException(ErrorType errorType) {
        super(errorType.getDefaultMessage());
        this.errorType = errorType;
        this.customMessage = null;
    }

    public ApiException(ErrorType errorType, String customMessage) {
        super(customMessage);
        this.errorType = errorType;
        this.customMessage = customMessage;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public String getDisplayMessage() {
        return customMessage != null ? customMessage : errorType.getDefaultMessage();
    }
}
