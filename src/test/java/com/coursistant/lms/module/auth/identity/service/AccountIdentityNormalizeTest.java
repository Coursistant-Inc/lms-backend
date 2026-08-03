package com.coursistant.lms.module.auth.identity.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountIdentityNormalizeTest {

    @Test
    void normalizeEmail_trimsAndLowercases() {
        assertEquals("a@b.com", AccountIdentityService.normalizeEmail("  A@B.COM "));
    }
}
