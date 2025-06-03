package com.coursistant.lms.utils;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.exception.CustomException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeZoneUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将本地时间（带时区）转换为 UTC 时间（LocalDateTime）
     * Convert local time in a timezone to UTC LocalDateTime
     */
    public static LocalDateTime toUtc(LocalDateTime localTime, String timezone) {
        if (localTime == null || timezone == null) {
            throw new IllegalArgumentException("Local time and timezone must not be null");
        }

        ZonedDateTime zonedTime = localTime.atZone(ZoneId.of(timezone));
        return zonedTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }


    /**
     * 将 UTC 时间转换为指定时区的本地时间字符串（用于展示）
     * Convert UTC time to local time string in a given timezone
     */
    public static String toIsoStringFromUtc(LocalDateTime utcTime, String timezone) {
        ZonedDateTime localZoned = utcTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of(timezone));
        return localZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
