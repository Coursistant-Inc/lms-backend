package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.dto.result.MyResultResponse;
import com.coursistant.lms.module.quiz.entity.QuizAttempt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class QuizResultVisibilityTest {

    @Test
    void afterRelease_hidesScoresUntilReleased() {
        MyResultResponse r = new MyResultResponse();
        QuizAttempt attempt = attemptWithScores();
        QuizResultService.applyVisibility(r, false, false, false, attempt);
        assertNull(r.getAutoScore());
        assertNull(r.getTotalScore());
    }

    @Test
    void afterRelease_showsScoresWhenReleasedAndComplete() {
        MyResultResponse r = new MyResultResponse();
        QuizAttempt attempt = attemptWithScores();
        QuizResultService.applyVisibility(r, false, true, false, attempt);
        assertEquals(new BigDecimal("7"), r.getAutoScore());
        assertEquals(new BigDecimal("3"), r.getManualScore());
        assertEquals(new BigDecimal("10"), r.getTotalScore());
    }

    @Test
    void instantAuto_showsAutoBeforeRelease() {
        MyResultResponse r = new MyResultResponse();
        QuizAttempt attempt = attemptWithScores();
        QuizResultService.applyVisibility(r, true, false, true, attempt);
        assertEquals(new BigDecimal("7"), r.getAutoScore());
        assertTrue(r.getManualGradingPending());
        assertNull(r.getTotalScore());
    }

    @Test
    void instantAuto_showsAllWhenReleasedAndComplete() {
        MyResultResponse r = new MyResultResponse();
        QuizAttempt attempt = attemptWithScores();
        QuizResultService.applyVisibility(r, true, true, false, attempt);
        assertEquals(new BigDecimal("10"), r.getTotalScore());
        assertFalse(r.getManualGradingPending());
    }

    private static QuizAttempt attemptWithScores() {
        QuizAttempt a = new QuizAttempt();
        a.setAutoScore(new BigDecimal("7"));
        a.setManualScore(new BigDecimal("3"));
        a.setTotalScore(new BigDecimal("10"));
        a.setManualGradingComplete(true);
        return a;
    }
}
