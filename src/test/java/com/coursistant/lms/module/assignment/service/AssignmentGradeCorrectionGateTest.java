package com.coursistant.lms.module.assignment.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentGradeCorrectionGateTest {

    @Test
    void visibleGradeFieldsChanged_detectsScoreDeltaIgnoringScale() {
        assertFalse(AssignmentGradingService.visibleGradeFieldsChanged(
                new BigDecimal("10.0"), "ok", new BigDecimal("10.00"), "ok"));
        assertTrue(AssignmentGradingService.visibleGradeFieldsChanged(
                new BigDecimal("10.0"), "ok", new BigDecimal("9.5"), "ok"));
    }

    @Test
    void visibleGradeFieldsChanged_detectsFeedbackDelta() {
        assertTrue(AssignmentGradingService.visibleGradeFieldsChanged(
                new BigDecimal("10"), "a", new BigDecimal("10"), "b"));
        assertFalse(AssignmentGradingService.visibleGradeFieldsChanged(
                new BigDecimal("10"), null, new BigDecimal("10"), null));
        assertTrue(AssignmentGradingService.visibleGradeFieldsChanged(
                new BigDecimal("10"), null, new BigDecimal("10"), "x"));
    }

    @Test
    void scoresEqual_nullSafe() {
        assertTrue(AssignmentGradingService.scoresEqual(null, null));
        assertFalse(AssignmentGradingService.scoresEqual(null, BigDecimal.ONE));
        assertFalse(AssignmentGradingService.scoresEqual(BigDecimal.ONE, null));
        assertTrue(AssignmentGradingService.scoresEqual(new BigDecimal("1.0"), new BigDecimal("1.00")));
    }
}
