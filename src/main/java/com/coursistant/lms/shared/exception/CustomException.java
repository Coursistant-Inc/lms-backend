package com.coursistant.lms.shared.exception;

import com.coursistant.lms.shared.enums.ResultCodeEnum;

/**
 * Legacy business exception for the old Result response style.
 * Replaced by {@link com.coursistant.lms.shared.api.ApiException}.
 * Delete after all modules are migrated to the new API style
 * (also remove the CustomException handler in ApiExceptionHandler).
 */
public class CustomException extends RuntimeException {
    private String code;
    private String msg;

    public CustomException(ResultCodeEnum resultCodeEnum) {
        this.code = resultCodeEnum.code;
        this.msg = resultCodeEnum.msg;
    }

    public CustomException(String code, String msg) {
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
