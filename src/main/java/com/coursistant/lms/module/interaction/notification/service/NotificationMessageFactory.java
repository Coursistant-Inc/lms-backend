package com.coursistant.lms.module.interaction.notification.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Builds student-facing notification messages. Never include numeric scores.
 */
@Component
public class NotificationMessageFactory {

    private static final int MAX_MESSAGE_LENGTH = 512;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    public String submissionReceived(String assignmentTitle, LocalDateTime submittedAt) {
        String title = blankToDefault(assignmentTitle, "Assignment");
        String when = submittedAt == null ? "" : submittedAt.toString();
        return truncate("Submission received: " + title + " at " + when + ".");
    }

    public String quizGradeCorrected(String quizTitle) {
        String title = blankToDefault(quizTitle, "Quiz");
        return truncate("Your quiz grade has been updated: " + title);
    }

    public String weekPublished(String weekTitle) {
        return truncate("New week published: " + blankToDefault(weekTitle, "Week"));
    }

    public String assignmentScheduleChanged(String assignmentTitle, String dueAt, String lateUntil) {
        String title = blankToDefault(assignmentTitle, "Assignment");
        StringBuilder message = new StringBuilder("Assignment deadline updated: ")
                .append(title)
                .append(". New due time: ")
                .append(blankToDefault(dueAt, "unspecified"))
                .append(".");
        if (lateUntil != null && !lateUntil.isBlank()) {
            message.append(" Late until: ").append(lateUntil).append(".");
        }
        return truncate(message.toString());
    }

    public String quizPublished(String quizTitle) {
        return truncate("New quiz published: " + blankToDefault(quizTitle, "Quiz"));
    }

    public String quizScheduleChanged(String quizTitle, String window) {
        return truncate("Quiz schedule updated: " + blankToDefault(quizTitle, "Quiz")
                + ". New availability: " + blankToDefault(window, "unspecified") + ".");
    }

    public String quizTimeLimitChanged(String quizTitle) {
        return truncate("Quiz time limit updated: " + blankToDefault(quizTitle, "Quiz") + ".");
    }

    public String courseEventCreated(String eventTitle, String eventTime) {
        return truncate("New course event: " + blankToDefault(eventTitle, "Event")
                + " at " + blankToDefault(eventTime, "unspecified") + ".");
    }

    public String groupMemberAddedTarget(String groupName) {
        return truncate("You joined group: " + blankToDefault(groupName, "Group"));
    }

    public String groupMemberAddedMembers(String userName, String groupName) {
        return truncate(blankToDefault(userName, "A student") + " joined group: "
                + blankToDefault(groupName, "Group"));
    }

    public String groupMemberRemovedTarget(String groupName) {
        return truncate("You left group: " + blankToDefault(groupName, "Group"));
    }

    public String groupMemberRemovedMembers(String userName, String groupName) {
        return truncate(blankToDefault(userName, "A student") + " left group: "
                + blankToDefault(groupName, "Group"));
    }

    public String groupMemberMovedTarget(String oldGroupName, String newGroupName) {
        return truncate("You moved from " + blankToDefault(oldGroupName, "a group")
                + " to " + blankToDefault(newGroupName, "a group"));
    }

    public String groupMemberMovedOldMembers(String userName, String oldGroupName) {
        return truncate(blankToDefault(userName, "A student") + " left group: "
                + blankToDefault(oldGroupName, "Group"));
    }

    public String groupMemberMovedNewMembers(String userName, String newGroupName) {
        return truncate(blankToDefault(userName, "A student") + " joined group: "
                + blankToDefault(newGroupName, "Group"));
    }

    public String formatUtc(LocalDateTime utc, ZoneId zone) {
        if (utc == null) {
            return "";
        }
        ZoneId target = zone == null ? ZoneOffset.UTC : zone;
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(target).toLocalDateTime().format(DISPLAY);
    }

    public String formatLocal(LocalDate date, LocalTime time) {
        if (date == null) {
            return "";
        }
        LocalTime t = time == null ? LocalTime.MIDNIGHT : time;
        return LocalDateTime.of(date, t).format(DISPLAY);
    }

    public String formatWindow(LocalDateTime opensAtUtc, LocalDateTime closesAtUtc, ZoneId zone) {
        return formatUtc(opensAtUtc, zone) + " – " + formatUtc(closesAtUtc, zone);
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
