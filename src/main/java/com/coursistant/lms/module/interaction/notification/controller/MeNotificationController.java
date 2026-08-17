package com.coursistant.lms.module.interaction.notification.controller;

import com.coursistant.lms.module.interaction.notification.dto.NotificationEmptyApiResponse;
import com.coursistant.lms.module.interaction.notification.dto.NotificationPageApiResponse;
import com.coursistant.lms.module.interaction.notification.dto.NotificationPageResponse;
import com.coursistant.lms.module.interaction.notification.dto.UnreadCountApiResponse;
import com.coursistant.lms.module.interaction.notification.dto.UnreadCountResponse;
import com.coursistant.lms.module.interaction.notification.service.NotificationService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/me/notifications")
@Tag(name = "MeNotifications", description = "Current-user in-app notifications")
public class MeNotificationController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Required idempotency key for safe retries of this mutating request "
                    + "(A-Za-z0-9_- , max 128). Missing/invalid keys return 400 "
                    + "IDEMPOTENCY_KEY_REQUIRED / IDEMPOTENCY_KEY_INVALID.";

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserMapper userMapper;

    @GetMapping
    @Operation(
            operationId = "meNotificationList",
            summary = "List my notifications",
            description = "Paged list for the authenticated user (newest first). "
                    + "page defaults to 1; size defaults to 20 and is capped at 100. "
                    + "Each item includes availability (AVAILABLE / NO_LONGER_AVAILABLE). "
                    + "Main errors: UNAUTHORIZED, USER_NOT_FOUND, BAD_REQUEST (missing tenant)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SUCCESS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationPageApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "UNAUTHORIZED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<NotificationPageResponse> list(HttpServletRequest request,
                                                      @Parameter(description = "1-based page; defaults to 1")
                                                      @RequestParam(required = false) Integer page,
                                                      @Parameter(description = "Page size; defaults to 20, max 100")
                                                      @RequestParam(required = false) Integer size) {
        Integer userId = currentUserId(request);
        requireUserTenant(userId);
        return ApiResponse.success(notificationService.list(userId, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(
            operationId = "meNotificationUnreadCount",
            summary = "Unread notification count",
            description = "Returns unreadCount for the authenticated user. "
                    + "Main errors: UNAUTHORIZED, USER_NOT_FOUND, BAD_REQUEST (missing tenant)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SUCCESS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UnreadCountApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "UNAUTHORIZED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<UnreadCountResponse> unreadCount(HttpServletRequest request) {
        Integer userId = currentUserId(request);
        requireUserTenant(userId);
        return ApiResponse.success(notificationService.unreadCount(userId));
    }

    @Idempotent
    @PatchMapping("/{notificationId}/read")
    @Operation(
            operationId = "meNotificationMarkRead",
            summary = "Mark one notification as read",
            description = "Idempotent via Idempotency-Key header. Cross-user or missing ids return 404 NOT_FOUND. "
                    + "Already-read notifications succeed with no change. "
                    + "Main errors: UNAUTHORIZED, USER_NOT_FOUND, NOT_FOUND, "
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
                    responseCode = "404",
                    description = "NOT_FOUND when the notification is not owned by the caller",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "IDEMPOTENCY_KEY_REQUIRED / IDEMPOTENCY_KEY_INVALID / BAD_REQUEST",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> markRead(HttpServletRequest request, @PathVariable Integer notificationId) {
        Integer userId = currentUserId(request);
        requireUserTenant(userId);
        notificationService.markRead(userId, notificationId);
        return ApiResponse.success();
    }

    @Idempotent
    @PatchMapping("/read-all")
    @Operation(
            operationId = "meNotificationMarkAllRead",
            summary = "Mark all my notifications as read",
            description = "Idempotent via Idempotency-Key header. Returns unreadCount=0. "
                    + "Main errors: UNAUTHORIZED, USER_NOT_FOUND, "
                    + "IDEMPOTENCY_KEY_REQUIRED, IDEMPOTENCY_KEY_INVALID.",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SUCCESS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UnreadCountApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "IDEMPOTENCY_KEY_REQUIRED / IDEMPOTENCY_KEY_INVALID / BAD_REQUEST",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<UnreadCountResponse> markAllRead(HttpServletRequest request) {
        Integer userId = currentUserId(request);
        requireUserTenant(userId);
        return ApiResponse.success(notificationService.markAllRead(userId));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * Loads the caller's tenant via UserMapper.selectById (tenant isolation gate).
     */
    private User requireUserTenant(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (user.getTenantId() == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "User tenant is required");
        }
        return user;
    }
}
