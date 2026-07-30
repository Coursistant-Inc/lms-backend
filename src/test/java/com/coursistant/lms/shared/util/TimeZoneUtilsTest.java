package com.coursistant.lms.shared.util;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeZoneUtilsTest {

    private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

    @Test
    void springForwardGap_throwsInvalidLocalTime() {
        // 2026-03-08 02:30 never exists in America/Los_Angeles
        LocalDateTime gap = LocalDateTime.of(2026, 3, 8, 2, 30);
        assertTrue(TimeZoneUtils.isAmbiguousOrGap(gap, LA));
        ApiException ex = assertThrows(ApiException.class,
                () -> TimeZoneUtils.toUtcLocalDateTime(gap, LA));
        assertEquals(ErrorType.INVALID_LOCAL_TIME, ex.getErrorType());
    }

    @Test
    void fallBackOverlap_throwsInvalidLocalTime() {
        // 2026-11-01 01:30 occurs twice in America/Los_Angeles
        LocalDateTime overlap = LocalDateTime.of(2026, 11, 1, 1, 30);
        assertTrue(TimeZoneUtils.isAmbiguousOrGap(overlap, LA));
        ApiException ex = assertThrows(ApiException.class,
                () -> TimeZoneUtils.toUtcLocalDateTime(overlap, LA));
        assertEquals(ErrorType.INVALID_LOCAL_TIME, ex.getErrorType());
    }

    @Test
    void validWallClock_convertsToUtc() {
        LocalDateTime local = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime utc = TimeZoneUtils.toUtcLocalDateTime(local, LA);
        assertEquals(LocalDateTime.of(2026, 8, 1, 7, 0), utc);
    }
}
