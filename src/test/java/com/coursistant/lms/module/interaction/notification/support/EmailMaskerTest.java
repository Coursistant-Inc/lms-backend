package com.coursistant.lms.module.interaction.notification.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmailMaskerTest {

    @Test
    void masksLocalAndDomain() {
        String masked = EmailMasker.mask("alice@example.com");
        assertEquals("a***@e***.com", masked);
        assertFalse(masked.contains("alice"));
        assertFalse(masked.contains("example"));
    }

    @Test
    void blank_returnsEmpty() {
        assertEquals("", EmailMasker.mask(" "));
        assertEquals("", EmailMasker.mask(null));
    }
}
