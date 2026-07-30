package com.coursistant.lms.shared.util;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.List;

/**
 * Wall-clock &lt;-&gt; UTC helpers. DB stores UTC as {@link LocalDateTime} without zone;
 * API {@code *Utc} fields use {@link Instant} (JSON with {@code Z}).
 */
@Component
public class TimeZoneUtils {

    public static LocalDateTime toUtcLocalDateTime(LocalDateTime local, ZoneId localZone) {
        if (local == null) {
            return null;
        }
        ZoneId zone = localZone == null ? ZoneOffset.UTC : localZone;
        ZoneRules rules = zone.getRules();
        List<ZoneOffset> offsets = rules.getValidOffsets(local);
        if (offsets.isEmpty()) {
            throw new ApiException(ErrorType.INVALID_LOCAL_TIME);
        }
        if (offsets.size() > 1) {
            throw new ApiException(ErrorType.INVALID_LOCAL_TIME);
        }
        return local.atOffset(offsets.get(0)).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime fromUtcLocalDateTime(LocalDateTime utcLocal, ZoneId targetZone) {
        if (utcLocal == null || targetZone == null) {
            return null;
        }
        return utcLocal.atZone(ZoneOffset.UTC).withZoneSameInstant(targetZone).toLocalDateTime();
    }

    public static Instant toInstant(LocalDateTime utcLocal) {
        if (utcLocal == null) {
            return null;
        }
        return utcLocal.toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime toUtcLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            throw new ApiException(ErrorType.INVALID_TIMEZONE);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            throw new ApiException(ErrorType.INVALID_TIMEZONE);
        }
    }

    /** Exposed for tests: whether {@code local} is a DST gap or overlap in {@code zone}. */
    public static boolean isAmbiguousOrGap(LocalDateTime local, ZoneId zone) {
        List<ZoneOffset> offsets = zone.getRules().getValidOffsets(local);
        return offsets.isEmpty() || offsets.size() > 1;
    }
}
