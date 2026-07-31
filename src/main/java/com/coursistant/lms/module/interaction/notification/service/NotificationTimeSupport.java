package com.coursistant.lms.module.interaction.notification.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Notification timestamps are stored as UTC {@link LocalDateTime}.
 */
@Component
public class NotificationTimeSupport {

    public LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
