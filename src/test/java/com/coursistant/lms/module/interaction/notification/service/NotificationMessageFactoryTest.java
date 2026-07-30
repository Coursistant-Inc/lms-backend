package com.coursistant.lms.module.interaction.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationMessageFactoryTest {

    private final NotificationMessageFactory factory = new NotificationMessageFactory();

    @Test
    void gradeMessages_doNotContainNumericScores() {
        String released = factory.assignmentGradeReleased("Homework 2");
        String corrected = factory.quizGradeCorrected("Midterm");
        assertFalse(released.matches(".*\\d+\\.?\\d*.*score.*|.*score.*\\d+.*"), released);
        assertFalse(released.contains("%"));
        assertFalse(corrected.contains("%"));
        assertTrue(released.contains("Homework 2"));
        assertTrue(corrected.toLowerCase().contains("updated") || corrected.toLowerCase().contains("corrected")
                || corrected.contains("Midterm"));
    }
}
