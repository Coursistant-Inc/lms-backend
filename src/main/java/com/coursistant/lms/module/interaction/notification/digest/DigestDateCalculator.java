package com.coursistant.lms.module.interaction.notification.digest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class DigestDateCalculator {

    private DigestDateCalculator() {
    }

    public static LocalDate digestDate(LocalDateTime occurredAtUtc, ZoneId zone) {
        ZoneId tz = zone != null ? zone : ZoneOffset.UTC;
        LocalDateTime occurred = occurredAtUtc != null ? occurredAtUtc : LocalDateTime.now(ZoneOffset.UTC);
        ZonedDateTime local = occurred.atZone(ZoneOffset.UTC).withZoneSameInstant(tz);
        LocalDate day = local.toLocalDate();
        return local.toLocalTime().isBefore(LocalTime.of(8, 0)) ? day : day.plusDays(1);
    }
}
