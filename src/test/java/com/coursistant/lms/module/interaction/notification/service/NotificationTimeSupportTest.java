package com.coursistant.lms.module.interaction.notification.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTimeSupportTest {

    private final NotificationTimeSupport support = new NotificationTimeSupport();
    private TimeZone previous;

    @AfterEach
    void restoreDefaultTimeZone() {
        if (previous != null) {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void nowUtc_usesUtcEvenWhenJvmDefaultIsNonUtc() {
        previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

        LocalDateTime actual = support.nowUtc();
        LocalDateTime expected = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

        long absSeconds = Math.abs(ChronoUnit.SECONDS.between(expected, actual));
        assertTrue(absSeconds <= 2,
                "nowUtc() should stay within ±2s of UTC now, but delta=" + absSeconds
                        + " actual=" + actual + " expected=" + expected);
    }
}
