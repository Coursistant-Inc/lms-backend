package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * X-Timezone handling for the assignment module. Everything is stored as UTC
 * {@code LocalDateTime}; the header only says how to interpret inbound wall-clock times and how
 * to render outbound ones.
 */
@Component
public class AssignmentTimeSupport {

    /**
     * Truncated to seconds so a timestamp echoed straight back in a response matches what a
     * later read returns: the DATETIME columns backing these values hold no sub-second precision.
     */
    public LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Resolves a required IANA timezone header. {@link TimeZoneUtils} still speaks the legacy
     * exception type, so translate it to the shared/api error contract.
     */
    public ZoneId requireZone(String timezoneHeader) {
        try {
            return TimeZoneUtils.resolveZoneId(timezoneHeader);
        } catch (CustomException e) {
            throw new ApiException(ErrorType.INVALID_TIMEZONE);
        }
    }

    /**
     * Resolves an optional header, falling back to UTC when absent.
     */
    public ZoneId zoneOrUtc(String timezoneHeader) {
        if (timezoneHeader == null || timezoneHeader.trim().isEmpty()) {
            return ZoneOffset.UTC;
        }
        return requireZone(timezoneHeader);
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
}
