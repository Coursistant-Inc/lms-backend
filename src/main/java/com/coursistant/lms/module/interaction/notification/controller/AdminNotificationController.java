package com.coursistant.lms.module.interaction.notification.controller;

import com.coursistant.lms.module.auth.identity.service.IdentityAuditService;
import com.coursistant.lms.module.interaction.notification.digest.DailyDigestService;
import com.coursistant.lms.module.interaction.notification.dto.DigestRunRequest;
import com.coursistant.lms.module.interaction.notification.dto.NotificationEmptyApiResponse;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.AuthzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v2/admin/notifications")
@Tag(name = "AdminNotifications", description = "SYSTEM_ADMIN notification operations")
public class AdminNotificationController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Required idempotency key for safe retries of this mutating request "
                    + "(A-Za-z0-9_- , max 128). Missing/invalid keys return 400 "
                    + "IDEMPOTENCY_KEY_REQUIRED / IDEMPOTENCY_KEY_INVALID.";

    @Resource
    private AuthzService authzService;

    @Resource
    private DailyDigestService dailyDigestService;

    @Resource
    private IdentityAuditService identityAuditService;

    @Idempotent
    @PostMapping("/digest/run")
    @Operation(
            operationId = "adminNotificationDigestRun",
            summary = "Manually run daily digest",
            description = "SYSTEM_ADMIN only. Idempotent via Idempotency-Key header. "
                    + "digestDate is required; tenantId is optional (omit to run all tenants). "
                    + "Main errors: FORBIDDEN, PARAM_MISSING, "
                    + "IDEMPOTENCY_KEY_REQUIRED, IDEMPOTENCY_KEY_INVALID.",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SUCCESS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationEmptyApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING when digestDate is missing; IDEMPOTENCY_KEY_REQUIRED / IDEMPOTENCY_KEY_INVALID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN when the caller is not SYSTEM_ADMIN",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> runDigest(HttpServletRequest request, @RequestBody DigestRunRequest body) {
        authzService.requireSystemAdmin(request);
        if (body == null || body.getDigestDate() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "digestDate is required");
        }
        LocalDate date = body.getDigestDate();
        Integer tenantId = body.getTenantId();
        dailyDigestService.run(date, tenantId);
        identityAuditService.writeSuccess(authzService.requireUserId(request), RoleEnum.SYSTEM_ADMIN.name(),
                null, "NOTIFICATION_DIGEST_RUN", "notification_digest", null, tenantId,
                null, date.toString(), null, request.getRemoteAddr());
        return ApiResponse.success();
    }
}
