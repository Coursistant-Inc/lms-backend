package com.coursistant.lms.module.interaction.notification.digest;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantTimeZoneResolver {

    private final ConcurrentHashMap<Integer, ZoneId> cache = new ConcurrentHashMap<>();

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private NotificationProperties notificationProperties;

    public ZoneId resolve(Integer tenantId) {
        if (tenantId == null) {
            return defaultZone();
        }
        return cache.computeIfAbsent(tenantId, this::load);
    }

    public LocalDate digestDate(Integer tenantId, LocalDateTime occurredAtUtc) {
        ZoneId zone = resolve(tenantId);
        LocalDateTime occurred = occurredAtUtc != null ? occurredAtUtc : LocalDateTime.now(ZoneOffset.UTC);
        ZonedDateTime local = occurred.atZone(ZoneOffset.UTC).withZoneSameInstant(zone);
        LocalDate day = local.toLocalDate();
        return local.toLocalTime().isBefore(LocalTime.of(8, 0)) ? day : day.plusDays(1);
    }

    private ZoneId load(Integer tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getTimezone() == null || tenant.getTimezone().isBlank()) {
            return defaultZone();
        }
        try {
            return ZoneId.of(tenant.getTimezone().trim());
        } catch (DateTimeException e) {
            return defaultZone();
        }
    }

    public ZoneId defaultZone() {
        String id = notificationProperties.getDigest().getDefaultTimeZone();
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            return ZoneId.of("America/Los_Angeles");
        }
    }
}
