package com.coursistant.lms.module.auth.unit;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.util.PasswordValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    @Test
    void accepts_minLengthWithLetterAndDigit() {
        assertDoesNotThrow(() -> PasswordValidator.validate("abcd1234"));
    }

    @Test
    void rejects_tooShort() {
        ApiException ex = assertThrows(ApiException.class, () -> PasswordValidator.validate("ab12"));
        assertEquals(ErrorType.INVALID_PASSWORD_FORMAT, ex.getErrorType());
    }

    @Test
    void rejects_lettersOnly() {
        assertThrows(ApiException.class, () -> PasswordValidator.validate("abcdefgh"));
    }

    @Test
    void rejects_digitsOnly() {
        assertThrows(ApiException.class, () -> PasswordValidator.validate("12345678"));
    }

    @Test
    void rejects_null() {
        assertThrows(ApiException.class, () -> PasswordValidator.validate(null));
    }

    @Test
    void accepts_specialCharactersWithLetterAndDigit() {
        assertDoesNotThrow(() -> PasswordValidator.validate("Abcdef1!"));
    }
}
