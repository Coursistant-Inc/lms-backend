package com.coursistant.lms.module.quiz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizDeadlineCalculationTest {

    private QuizTimeSupport timeSupport;

    @BeforeEach
    void setUp() {
        timeSupport = new QuizTimeSupport();
    }

    @Test
    void noTimeLimit_usesClosesAt() {
        LocalDateTime started = LocalDateTime.of(2026, 7, 25, 10, 0);
        LocalDateTime closes = LocalDateTime.of(2026, 7, 25, 12, 0);
        assertEquals(closes, timeSupport.computeDeadline(started, closes, null));
    }

    @Test
    void timeLimitBeforeClose_usesLimit() {
        LocalDateTime started = LocalDateTime.of(2026, 7, 25, 10, 0);
        LocalDateTime closes = LocalDateTime.of(2026, 7, 25, 12, 0);
        assertEquals(LocalDateTime.of(2026, 7, 25, 10, 30),
                timeSupport.computeDeadline(started, closes, 1800));
    }

    @Test
    void timeLimitAfterClose_usesClose() {
        LocalDateTime started = LocalDateTime.of(2026, 7, 25, 10, 0);
        LocalDateTime closes = LocalDateTime.of(2026, 7, 25, 10, 20);
        assertEquals(closes, timeSupport.computeDeadline(started, closes, 3600));
    }
}
