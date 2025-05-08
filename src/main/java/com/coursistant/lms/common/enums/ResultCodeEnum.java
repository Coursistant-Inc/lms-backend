package com.coursistant.lms.common.enums;

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
    CHAT_NOT_EXIST_ERROR("4043", "Chat Not Exist"),
    INVITATION_NOT_EXIST_ERROR("4044", "Invitation Not Exist"),
    ASSIGNMENT_NOT_EXIST_ERROR("4045", "Assignment Not Exist"),
    SUBMISSION_NOT_EXIST_ERROR("4046", "Submission Not Exist"),
    SUBMISSION_NOT_VALID_ERROR("4046", "Submission Not Valid"),

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
