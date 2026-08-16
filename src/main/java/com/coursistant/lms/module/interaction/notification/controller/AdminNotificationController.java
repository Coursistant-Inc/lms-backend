package com.coursistant.lms.module.interaction.notification.controller;

import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.service.NotificationDeliveryOpsService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v2/admin/notifications")
@Tag(name = "AdminNotifications", description = "SYSTEM_ADMIN notification operations")
public class AdminNotificationController {

    public static class DigestRunRequest {
        public LocalDate digestDate;
        public Integer tenantId;
    }

    public static class RequeueDryRunRequest {
        public LocalDateTime from;
        public LocalDateTime to;
        public Integer tenantId;
        public String channel;
    }

    @Resource
    private AuthzService authzService;

    @Resource
    private DailyDigestService dailyDigestService;

    @Resource
    private NotificationDeliveryOpsService notificationDeliveryOpsService;

    @Resource
    private IdentityAuditService identityAuditService;

    @Idempotent
    @PostMapping("/digest/run")
    @Operation(operationId = "adminNotificationDigestRun", summary = "Manually run daily digest")
    public ApiResponse<Void> runDigest(HttpServletRequest request, @RequestBody(required = false) DigestRunRequest body) {
        authzService.requireSystemAdmin(request);
        LocalDate date = body == null ? null : body.digestDate;
        Integer tenantId = body == null ? null : body.tenantId;
        dailyDigestService.run(date, tenantId);
        identityAuditService.writeSuccess(authzService.requireUserId(request), RoleEnum.SYSTEM_ADMIN.name(),
                null, "NOTIFICATION_DIGEST_RUN", "notification_digest", null, tenantId,
                null, date == null ? null : date.toString(), null, request.getRemoteAddr());
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/deliveries/{id}/retry")
    @Operation(operationId = "adminNotificationDeliveryRetry", summary = "Requeue a failed delivery")
    public ApiResponse<Void> retryDelivery(HttpServletRequest request, @PathVariable Long id) {
        authzService.requireSystemAdmin(request);
        notificationDeliveryOpsService.retryDelivery(id);
        identityAuditService.writeSuccess(authzService.requireUserId(request), RoleEnum.SYSTEM_ADMIN.name(),
                null, "NOTIFICATION_DELIVERY_RETRY", "notification_delivery", id == null ? null : id.intValue(),
                null, null, null, null, request.getRemoteAddr());
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/deliveries/requeue-dry-run")
    @Operation(operationId = "adminNotificationRequeueDryRun", summary = "Requeue DRY_RUN deliveries after log canary")
    public ApiResponse<Void> requeueDryRun(HttpServletRequest request, @RequestBody RequeueDryRunRequest body) {
        authzService.requireSystemAdmin(request);
        LocalDateTime from = body == null ? LocalDateTime.MIN : body.from;
        LocalDateTime to = body == null ? LocalDateTime.MAX : body.to;
        Integer tenantId = body == null ? null : body.tenantId;
        String channel = body == null ? null : body.channel;
        notificationDeliveryOpsService.requeueDryRun(from, to, tenantId, channel);
        identityAuditService.writeSuccess(authzService.requireUserId(request), RoleEnum.SYSTEM_ADMIN.name(),
                null, "NOTIFICATION_REQUEUE_DRY_RUN", "notification_delivery", null, tenantId,
                null, null, null, request.getRemoteAddr());
        return ApiResponse.success();
    }
}
