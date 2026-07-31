package com.coursistant.lms.shared.enums;

/**
 * Legacy business error codes for the old Result response style.
 * Replaced by {@link com.coursistant.lms.shared.api.ErrorType}.
 * Delete after all modules are migrated to the new API style.
 */
public enum ResultCodeEnum {

    /**
     * Common success status
     */
    SUCCESS("200", "Success"),

    /**
     * Request parameter-related errors (400X)
     */
    PARAM_ERROR("4000", "Parameter Exception"),
    PARAM_LOST_ERROR("4001", "Parameter Missing"),
    PARAM_PASSWORD_ERROR("4002", "Incorrect Original Password"),
    PARAM_STATUS_ERROR("4003", "Please Select Review Status"),
    FILE_CATEGORY_MISMATCH("4004", "File and Category Count Mismatch"),
    INVALID_TIMEZONE("4005", "Invalid or Missing Timezone Header"),
    GOOGLE_DRIVE_NOT_AUTHORIZED("4006", "GOOGLE_DRIVE_NOT_AUTHORIZED"),

    /**
     * Authentication and authorization errors (401X)
     */
    TOKEN_INVALID_ERROR("4010", "Invalid Access Token"),
    TOKEN_CHECK_ERROR("4011", "Access Token Validation Failed"),
    USER_NOT_LOGIN("4012", "User Not Logged In"),
    USER_ACCOUNT_ERROR("4013", "Account or Password Incorrect"),
    VERIFICATION_CODE_ERROR("4014", "Incorrect Verification Code"),
    TOKEN_CREATION_ERROR("4015", "Error When Creating Token"),
    REFRESH_TOKEN_CHECK_ERROR("4016", "Refresh Token Validation Failed"),
    INVALID_ACCESS_ERROR("4017", "User Role Invalid for Access"),

    /**
     * User-related errors (402X)
     */
    USER_EXIST_ERROR("4020", "Username Already Exists"),
    USER_NOT_EXIST_ERROR("4021", "User Does Not Exist"),
    /**
     * File-related errors (403X)
     */
    TYPE_NOT_SUPPORT("4030", "File Type Not Supported For Online Preview"),
    FILE_DUPLICATE_ERROR("4031", "File Duplicate"),
    FILE_READ_ERROR("4032", "Cannot Read File"),
    FILE_NOT_FOUND("4033", "File Not Found"),
    FILE_UPLOAD_ERROR("4034", "File upload error"),
    FILE_DELETION_ERROR("4035", "File deletion error"),
    FILE_LIMIT_EXCEEDED("4036","File size limit exceeded"),

    /**
     * System errors (500X)
     */
    SYSTEM_ERROR("5000", "System Exception"),

    /**
     * Relation or data not found errors (404X)
     */
    TEACH_NOT_EXIST_ERROR("4040", "Teach Relation Not Exist"),
    LEARN_NOT_EXIST_ERROR("4041", "Learn Relation Not Exist"),
    COURSE_NOT_EXIST_ERROR("4042", "Course Not Exist"),
    INVITATION_NOT_EXIST_ERROR("4044", "Invitation Not Exist"),
    ASSIGNMENT_NOT_EXIST_ERROR("4045", "Assignment Not Exist"),
    SUBMISSION_NOT_EXIST_ERROR("4046", "Submission Not Exist"),

    SUBMISSION_DUE_EXPIRED_ERROR("4047", "Submission Due Date Expired"),
    SUBMISSION_ATTEMPT_EXCEEDED_ERROR("4051", "Submission Attempts Exceeded"),

    EVENT_NOT_EXIST_ERROR("4048", "Event Not Exist"),
    GROUP_NOT_EXIST_ERROR("4050", "Group Not Exist"),

    /**
     * Invalid operation related errors (405X)
     */
    GROUP_JOIN_INVALID_ERROR("4050", "Group Is Full/Not Open for Join"),
    DUPLICATED_GROUP_MEMBER_ERROR("4051", "User Already In Group"),
    DUPLICATED_LEARN_RELATION_ERROR("4052", "User Already In This Course"),
    DUPLICATED_JOIN_REQUEST_ERROR("4053", "Join Request Already Exists"),
    REQUEST_NOT_EXIST_ERROR("4054", "Join Request Not Exist"),
    REQUEST_ALREADY_PROCESSED_ERROR("4055", "Join Request Already Processed"),
    NO_PERMISSION_ERROR("4056", "No Permission to Perform This Action"),

    /**
     * External service errors (600X)
     */
    EMAIL_NOT_SEND_ERROR("6000", "Fail To Send Email")


    ;

    public String code;
    public String msg;

    ResultCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
