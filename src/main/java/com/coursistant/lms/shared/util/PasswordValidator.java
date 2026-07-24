package com.coursistant.lms.shared.util;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;

import java.util.regex.Pattern;

public final class PasswordValidator {

    private static final Pattern PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).{8,}$");

    private PasswordValidator() {
    }

    public static void validate(String password) {
        if (password == null || !PATTERN.matcher(password).matches()) {
            throw new ApiException(ErrorType.INVALID_PASSWORD_FORMAT,
                    "Password must be at least 8 characters and contain both letters and numbers");
        }
    }
}
