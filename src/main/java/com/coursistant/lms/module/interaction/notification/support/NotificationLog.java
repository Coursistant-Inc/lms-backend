package com.coursistant.lms.module.interaction.notification.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NotificationLog {

    private static final Logger log = LoggerFactory.getLogger("notification");

    public static final int LAST_ERROR_MAX_LENGTH = 512;

    private NotificationLog() {
    }

    public static String truncateLastError(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > LAST_ERROR_MAX_LENGTH
                ? value.substring(0, LAST_ERROR_MAX_LENGTH)
                : value;
    }

    public static void info(String event, String eventId, Integer tenantId, String eventType,
                            String channel, String status, String failureCategory,
                            String providerMessageId, Integer recipientUserId, Integer attempt,
                            String claimToken) {
        log.info("notification event={} eventId={} tenantId={} eventType={} channel={} status={} "
                        + "failureCategory={} providerMessageId={} recipientUserId={} attempt={} claimed={}",
                event, eventId, tenantId, eventType, channel, status, failureCategory,
                providerMessageId, recipientUserId, attempt, claimed(claimToken));
    }

    public static void warn(String event, String eventId, Integer tenantId, String eventType,
                            String channel, String status, String failureCategory,
                            String providerMessageId, Integer recipientUserId, Integer attempt,
                            String claimToken, String detail) {
        log.warn("notification event={} eventId={} tenantId={} eventType={} channel={} status={} "
                        + "failureCategory={} providerMessageId={} recipientUserId={} attempt={} claimed={} detail={}",
                event, eventId, tenantId, eventType, channel, status, failureCategory,
                providerMessageId, recipientUserId, attempt, claimed(claimToken), detail);
    }

    private static String claimed(String claimToken) {
        return claimToken == null || claimToken.isBlank() ? "absent" : "present";
    }

    public static Logger logger() {
        return log;
    }
}
