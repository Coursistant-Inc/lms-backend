package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.shared.util.TimeZoneUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Assignment times are stored as UTC {@link LocalDateTime}. Wall-clock request fields are
 * interpreted in the course tenant timezone (not a request header).
 */
@Component
public class AssignmentTimeSupport {

    public LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    public LocalDateTime toUtc(LocalDateTime localWallClock, ZoneId zone) {
        if (localWallClock == null) {
            return null;
        }
        return TimeZoneUtils.toUtcLocalDateTime(localWallClock, zone == null ? ZoneOffset.UTC : zone);
    }

    public LocalDateTime toZone(LocalDateTime utc, ZoneId zone) {
        if (utc == null || zone == null) {
            return null;
        }
        return TimeZoneUtils.fromUtcLocalDateTime(utc, zone);
    }

    public Instant toInstant(LocalDateTime utc) {
        return TimeZoneUtils.toInstant(utc);
    }
}
