package com.coursistant.lms.module.interaction.notification.service;

/**
 * Canonical outbox event keys. Identity uses UTC/version ids, never formatted copy.
 */
public final class NotificationEventKeys {

    private NotificationEventKeys() {
    }

    public static String announcementPublished(Integer announcementId) {
        return "announcement:" + announcementId + ":publication:1";
    }

    public static String weekPublished(Integer weekId, Integer publicationVersion) {
        return "week:" + weekId + ":publication:" + publicationVersion;
    }

    public static String assignmentPublished(Integer assignmentId, Integer publicationVersion) {
        return "assignment:" + assignmentId + ":publication:" + publicationVersion;
    }

    public static String assignmentSchedule(Integer assignmentId, Integer scheduleVersion) {
        return "assignment:" + assignmentId + ":schedule:" + scheduleVersion;
    }

    public static String quizPublished(Integer quizId, Integer publicationVersion) {
        return "quiz:" + quizId + ":publication:" + publicationVersion;
    }

    public static String quizSchedule(Integer quizId, Integer quizVersion) {
        return "quiz:" + quizId + ":schedule:" + quizVersion;
    }

    public static String quizTimeLimit(Integer quizId, Integer quizVersion) {
        return "quiz:" + quizId + ":time-limit:" + quizVersion;
    }

    public static String courseEventCreated(Integer eventId) {
        return "course-event:" + eventId + ":created";
    }

    public static String groupAdded(Integer auditId, String variant) {
        return "group-membership-change:" + auditId + ":added:" + variant;
    }

    public static String groupRemoved(Integer auditId, String variant) {
        return "group-membership-change:" + auditId + ":removed:" + variant;
    }

    public static String groupMoved(Integer auditId, String variant) {
        return "group-membership-change:" + auditId + ":moved:" + variant;
    }
}
