package com.coursistant.lms.shared.api;

import org.springframework.http.HttpStatus;

public enum ErrorType {

    // --- Generic ---
    SUCCESS(HttpStatus.OK, "Success"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request data"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    // --- Auth: User ---
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Username Already Exists"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User Does Not Exist"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "Account is locked"),

    // --- Auth: Token ---
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid Access Token"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh Token Validation Failed"),
    TOKEN_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Error When Creating Token"),

    // --- Auth: Params ---
    PARAM_MISSING(HttpStatus.BAD_REQUEST, "Parameter Missing"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "Incorrect Original Password"),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "Incorrect Verification Code"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "No Permission to Perform This Action"),
    INVITATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "Invitation Not Exist"),

    // --- Auth: Verification ---
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "Password does not meet requirements"),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "Verification code expired"),
    VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect attempts"),
    VERIFICATION_RESEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting a new code"),
    VERIFICATION_HOURLY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "Hourly verification limit reached"),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send email"),

    // --- Idempotency ---
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required"),
    IDEMPOTENCY_KEY_INVALID(HttpStatus.BAD_REQUEST, "Idempotency-Key format is invalid"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "A request with this key is already being processed"),
    IDEMPOTENCY_KEY_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key was reused with a different request body"),

    // --- Profile ---
    INVALID_AVATAR_FILE(HttpStatus.BAD_REQUEST, "Invalid avatar file"),

    // --- Course ---
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "Course does not exist"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Course session does not exist"),
    COURSE_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Course event does not exist"),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Enrollment does not exist"),
    ENROLLMENT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "Enrollment is not active"),
    NOT_COURSE_INSTRUCTOR(HttpStatus.FORBIDDEN, "Not the course instructor"),
    NOT_COURSE_MEMBER(HttpStatus.FORBIDDEN, "Not a member of this course"),
    INVALID_ROLE_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid enrollment role transition"),
    COURSE_ARCHIVED(HttpStatus.BAD_REQUEST, "Course is archived"),
    SYLLABUS_NOT_FOUND(HttpStatus.NOT_FOUND, "Syllabus does not exist"),
    WEEK_NOT_FOUND(HttpStatus.NOT_FOUND, "Course week does not exist"),
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Course material does not exist"),
    WEEK_NOT_EMPTY(HttpStatus.CONFLICT, "Week still contains materials"),

    // --- Course Content Files ---
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "File exceeds the maximum allowed size"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "Unsupported file type"),
    INVALID_LINK(HttpStatus.BAD_REQUEST, "Invalid link title or URL"),
    NO_PREVIOUS_SYLLABUS_VERSION(HttpStatus.CONFLICT, "No previous syllabus version to restore");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorType(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public Integer getStatusCode() {
        return httpStatus.value();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
