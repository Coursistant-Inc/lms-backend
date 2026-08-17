package com.coursistant.lms.module.interaction.notification.controller;

import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v2/admin/notifications")
@Tag(name = "AdminNotifications", description = "SYSTEM_ADMIN notification operations")
public class AdminNotificationController {

    public static class DigestRunRequest {
        public LocalDate digestDate;
        public Integer tenantId;
    }

    @Resource
    private AuthzService authzService;

    @Resource
    private DailyDigestService dailyDigestService;

    @Resource
    private IdentityAuditService identityAuditService;

    @Idempotent
    @PostMapping("/digest/run")
    @Operation(operationId = "adminNotificationDigestRun", summary = "Manually run daily digest")
    public ApiResponse<Void> runDigest(HttpServletRequest request, @RequestBody DigestRunRequest body) {
        authzService.requireSystemAdmin(request);
        if (body == null || body.digestDate == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "digestDate is required");
        }
        LocalDate date = body.digestDate;
        Integer tenantId = body.tenantId;
        dailyDigestService.run(date, tenantId);
        identityAuditService.writeSuccess(authzService.requireUserId(request), RoleEnum.SYSTEM_ADMIN.name(),
                null, "NOTIFICATION_DIGEST_RUN", "notification_digest", null, tenantId,
                null, date.toString(), null, request.getRemoteAddr());
        return ApiResponse.success();
    }
}
