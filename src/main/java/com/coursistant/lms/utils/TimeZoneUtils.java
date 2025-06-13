package com.coursistant.lms.utils;

import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.exception.CustomException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.*;
import java.time.format.DateTimeFormatter;

@Component
public class TimeZoneUtils {

    /**
     * 将某个时区的 LocalDateTime 转换成对应 UTC 的 LocalDateTime
     * Convert a local datetime with a specific zone to its equivalent UTC time (still as LocalDateTime)
     */
    public static LocalDateTime toUtcLocalDateTime(LocalDateTime local, ZoneId localZone) {
        return local
                .atZone(localZone)                  // 先绑定时区
                .withZoneSameInstant(ZoneOffset.UTC) // 转为 UTC
                .toLocalDateTime();                 // 去掉时区，返回 LocalDateTime 表示的 UTC 时间
    }

    /**
     * 将 UTC 的 LocalDateTime 转换回指定时区的 LocalDateTime
     * Convert a UTC-localdatetime (no zone info) back to a specific time zone's local time
     */
    public static LocalDateTime fromUtcLocalDateTime(LocalDateTime utcLocal, ZoneId targetZone) {
        return utcLocal
                .atZone(ZoneOffset.UTC)             // 声明它是 UTC 时间
                .withZoneSameInstant(targetZone)    // 转换到目标时区
                .toLocalDateTime();                 // 去掉时区信息
    }

    /**
     * 解析并验证时区字符串（IANA 格式），若为空或非法则抛出自定义异常
     *
     * @param timezoneHeader 从请求头传入的时区字符串
     * @return 解析后的 ZoneId 对象
     */
    public static ZoneId resolveZoneId(String timezoneHeader) {
        if (timezoneHeader == null || timezoneHeader.trim().isEmpty()) {
            // 空或只包含空格
            throw new CustomException(ResultCodeEnum.INVALID_TIMEZONE);
        }
        try {
            return ZoneId.of(timezoneHeader.trim());
        } catch (DateTimeException e) {
            // 非法时区字符串
            throw new CustomException(ResultCodeEnum.INVALID_TIMEZONE);
        }
    }
}
