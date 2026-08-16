package com.coursistant.lms.module.interaction.notification.digest;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DigestDateCalculatorTest {

    @Test
    void beforeEight_sameLocalDay() {
        LocalDateTime utc = LocalDateTime.of(2026, 8, 16, 14, 59, 0);
        LocalDate date = DigestDateCalculator.digestDate(utc, ZoneId.of("America/Los_Angeles"));
        assertEquals(LocalDate.of(2026, 8, 16), date);
    }

    @Test
    void atEight_nextLocalDay() {
        LocalDateTime utc = LocalDateTime.of(2026, 8, 16, 15, 0, 0);
        LocalDate date = DigestDateCalculator.digestDate(utc, ZoneId.of("America/Los_Angeles"));
        assertEquals(LocalDate.of(2026, 8, 17), date);
    }

    @Test
    void shanghai_beforeEight() {
        LocalDateTime utc = LocalDateTime.of(2026, 8, 15, 23, 59, 0);
        LocalDate date = DigestDateCalculator.digestDate(utc, ZoneId.of("Asia/Shanghai"));
        assertEquals(LocalDate.of(2026, 8, 16), date);
    }

    @Test
    void utc_atEightGoesNextDay() {
        LocalDateTime utc = LocalDateTime.of(2026, 8, 16, 8, 0, 0);
        assertEquals(LocalDate.of(2026, 8, 17), DigestDateCalculator.digestDate(utc, ZoneId.of("UTC")));
    }

    @Test
    void dstSpringForward_losAngeles() {
        LocalDateTime utc = LocalDateTime.of(2026, 3, 8, 14, 30, 0);
        assertEquals(LocalDate.of(2026, 3, 8),
                DigestDateCalculator.digestDate(utc, ZoneId.of("America/Los_Angeles")));
    }
}
