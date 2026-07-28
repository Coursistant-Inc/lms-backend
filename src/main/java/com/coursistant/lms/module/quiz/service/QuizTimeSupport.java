package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class QuizTimeSupport {

    public LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }

    public ZoneId requireZone(String timezoneHeader) {
        try {
            return TimeZoneUtils.resolveZoneId(timezoneHeader);
        } catch (CustomException e) {
            throw new ApiException(ErrorType.INVALID_TIMEZONE);
        }
    }

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

    public LocalDateTime computeDeadline(LocalDateTime startedAt, LocalDateTime closesAt, Integer timeLimitSeconds) {
        if (timeLimitSeconds == null || timeLimitSeconds <= 0) {
            return closesAt;
        }
        LocalDateTime limitEnd = startedAt.plusSeconds(timeLimitSeconds);
        return limitEnd.isBefore(closesAt) ? limitEnd : closesAt;
    }
}
