package com.coursistant.lms.module.interaction.notification.service;

import org.springframework.stereotype.Component;

/**
 * Builds student-facing notification messages. Never include numeric scores.
 */
@Component
public class NotificationMessageFactory {

    private static final int MAX_MESSAGE_LENGTH = 512;

    public String announcementPosted(String announcementTitle) {
        String title = blankToDefault(announcementTitle, "New announcement");
        return truncate("New announcement: " + title);
    }

    public String assignmentPublished(String assignmentTitle) {
        String title = blankToDefault(assignmentTitle, "Assignment");
        return truncate("New assignment published: " + title);
    }

    public String assignmentGradeReleased(String assignmentTitle) {
        String title = blankToDefault(assignmentTitle, "Assignment");
        return truncate("Your assignment grade has been released: " + title);
    }

    public String quizGradeReleased(String quizTitle) {
        String title = blankToDefault(quizTitle, "Quiz");
        return truncate("Your quiz grade has been released: " + title);
    }

    public String assignmentGradeCorrected(String assignmentTitle) {
        String title = blankToDefault(assignmentTitle, "Assignment");
        return truncate("Your assignment grade has been updated: " + title);
    }

    public String submissionReceived(String assignmentTitle, java.time.LocalDateTime submittedAt) {
        String title = blankToDefault(assignmentTitle, "Assignment");
        String when = submittedAt == null ? "" : submittedAt.toString();
        return truncate("Submission received: " + title + " at " + when + ".");
    }

    public String quizGradeCorrected(String quizTitle) {
        String title = blankToDefault(quizTitle, "Quiz");
        return truncate("Your quiz grade has been updated: " + title);
    }

    private String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String truncate(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
