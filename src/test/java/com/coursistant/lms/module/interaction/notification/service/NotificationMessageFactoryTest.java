package com.coursistant.lms.module.interaction.notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void phase2Copy_includesTitlesAndGroupVariants() {
        NotificationMessageFactory factory = new NotificationMessageFactory();
        assertTrue(factory.weekPublished("W1").contains("W1"));
        assertTrue(factory.assignmentScheduleChanged("HW", "2026-08-17 09:00", "2026-08-18 09:00")
                .contains("lateUntil".replace("lateUntil", "Late until")));
        assertTrue(factory.quizPublished("Q1").contains("Q1"));
        assertTrue(factory.quizScheduleChanged("Q1", "open-close").contains("open-close"));
        assertTrue(factory.quizTimeLimitChanged("Q1").contains("Q1"));
        assertTrue(factory.courseEventCreated("Lab", "2026-08-17 10:00").contains("Lab"));
        assertTrue(factory.groupMemberAddedTarget("Alpha").contains("You joined"));
        assertTrue(factory.groupMemberMovedTarget("Old", "New").contains("Old"));
        assertTrue(factory.groupMemberMovedTarget("Old", "New").contains("New"));
        assertEquals(512, factory.weekPublished("x".repeat(600)).length());
    }
}
