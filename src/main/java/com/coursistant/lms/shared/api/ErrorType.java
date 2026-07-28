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
    ANNOUNCEMENT_GONE(HttpStatus.GONE, "Content no longer available"),
    ANNOUNCEMENT_DELETE_CONFIRM_REQUIRED(HttpStatus.CONFLICT, "Confirmation required to delete announcement"),

    // --- Group ---
    GROUP_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "Group set does not exist"),
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Group does not exist"),
    GROUP_NOT_EMPTY(HttpStatus.CONFLICT, "Group is not empty"),
    GROUP_SET_NOT_EMPTY(HttpStatus.CONFLICT, "Group set is not empty"),
    GROUP_CAPACITY_FULL(HttpStatus.CONFLICT, "Group just filled up"),
    GROUP_ALREADY_IN_SET(HttpStatus.CONFLICT, "Student is already in a group in this set"),
    GROUP_WINDOW_CLOSED(HttpStatus.CONFLICT, "Group join window is closed"),
    GROUP_LOCKED(HttpStatus.CONFLICT, "Group set is locked"),
    GROUP_ACADEMIC_HOLD(HttpStatus.CONFLICT, "Group membership change is blocked by academic hold"),
    GROUP_CAPACITY_CONFIRM_REQUIRED(HttpStatus.CONFLICT, "Confirmation required to exceed group capacity"),
    GROUP_ACADEMIC_CONFIRM_REQUIRED(HttpStatus.CONFLICT, "Confirmation required due to academic impact"),
    GROUP_SET_IN_USE(HttpStatus.CONFLICT, "Group set is referenced by an assignment"),
    GROUP_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "Group has assignment submissions and cannot be deleted"),
    NO_GROUP_MEMBERSHIP(HttpStatus.CONFLICT, "You must join or be assigned to a group before submitting"),

    // --- Course Content Files ---
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "File exceeds the maximum allowed size"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "Unsupported file type"),
    INVALID_LINK(HttpStatus.BAD_REQUEST, "Invalid link title or URL"),
    NO_PREVIOUS_SYLLABUS_VERSION(HttpStatus.CONFLICT, "No previous syllabus version to restore"),

    // --- Assignment ---
    ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Assignment does not exist"),
    ASSIGNMENT_TYPE_LOCKED(HttpStatus.CONFLICT, "Assignment type is locked"),
    ASSIGNMENT_GROUP_SET_REQUIRED(HttpStatus.CONFLICT, "Group assignment requires a linked group set"),
    ASSIGNMENT_HAS_SUBMISSIONS(HttpStatus.CONFLICT, "Assignment already has submissions"),
    ASSIGNMENT_DUE_SHORTEN_CONFIRM_REQUIRED(HttpStatus.CONFLICT, "Confirmation required to shorten due date"),
    ASSIGNMENT_FILE_CONSTRAINT_INVALID(HttpStatus.BAD_REQUEST, "Invalid assignment file constraints"),
    RUBRIC_NOT_FOUND(HttpStatus.NOT_FOUND, "Rubric does not exist"),
    RUBRIC_NO_PREVIOUS_VERSION(HttpStatus.NOT_FOUND, "No previous rubric version to restore"),
    RUBRIC_REPLACE_CONFIRM_REQUIRED(HttpStatus.CONFLICT, "Confirmation required to replace rubric after grading"),
    INVALID_TIMEZONE(HttpStatus.BAD_REQUEST, "Invalid or missing X-Timezone header"),
    SUBMISSION_WINDOW_CLOSED(HttpStatus.CONFLICT, "Submission window is closed"),
    SUBMISSION_FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "Too many submission files"),
    STAGING_FILE_INVALID(HttpStatus.BAD_REQUEST, "Staging file is invalid or already consumed"),
    SUBMISSION_FROZEN(HttpStatus.FORBIDDEN, "Assignment submission is frozen for this enrollment"),
    NOT_IN_GRADING_ROSTER(HttpStatus.NOT_FOUND, "Student is not in the grading roster"),
    GRADE_SCORE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "Grade score is out of range"),
    GRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "Grade does not exist"),
    STORAGE_FAILURE(HttpStatus.SERVICE_UNAVAILABLE, "Object storage operation failed"),

    // --- Quiz ---
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "Quiz does not exist"),
    QUIZ_NOT_PUBLISHED(HttpStatus.NOT_FOUND, "Quiz is not published"),
    QUIZ_WINDOW_CLOSED(HttpStatus.CONFLICT, "Quiz window is closed"),
    QUIZ_ATTEMPT_NOT_FOUND(HttpStatus.NOT_FOUND, "Quiz attempt does not exist"),
    QUIZ_ATTEMPT_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "Quiz attempt is not in progress"),
    QUIZ_CONTENT_LOCKED(HttpStatus.CONFLICT, "Quiz content is locked"),
    QUIZ_VERSION_CONFLICT(HttpStatus.CONFLICT, "Quiz version conflict"),
    QUIZ_HAS_ATTEMPTS(HttpStatus.CONFLICT, "Quiz already has attempts"),
    QUIZ_ANSWER_INVALID(HttpStatus.BAD_REQUEST, "Quiz answer is invalid"),
    QUIZ_ATTEMPTS_EXCEEDED(HttpStatus.CONFLICT, "Quiz attempt limit reached"),
    QUIZ_FEATURE_DISABLED(HttpStatus.CONFLICT, "Quiz new activity is disabled"),
    QUIZ_GRADING_FORBIDDEN(HttpStatus.FORBIDDEN, "Not permitted to grade this quiz"),
    QUIZ_TA_SELF_CONFLICT(HttpStatus.FORBIDDEN, "TA cannot grade a quiz they attempted"),
    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Quiz question does not exist"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

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
