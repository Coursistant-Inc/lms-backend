package com.coursistant.lms.module.interaction.notification.controller;

import com.coursistant.lms.module.interaction.notification.dto.NotificationPageResponse;
import com.coursistant.lms.module.interaction.notification.dto.UnreadCountResponse;
import com.coursistant.lms.module.interaction.notification.service.NotificationService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/me/notifications")
public class MeNotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserMapper userMapper;

    @GetMapping
    public ApiResponse<NotificationPageResponse> list(HttpServletRequest request,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        Integer userId = currentUserId(request);
        requireUserTenant(userId);
        return ApiResponse.success(notificationService.list(userId, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(HttpServletRequest request) {
        Integer userId = currentUserId(request);
        requireUserTenant(userId);
        return ApiResponse.success(notificationService.unreadCount(userId));
    }

    @Idempotent
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(HttpServletRequest request, @PathVariable Integer notificationId) {
        Integer userId = currentUserId(request);
        requireUserTenant(userId);
        notificationService.markRead(userId, notificationId);
        return ApiResponse.success();
    }

    @Idempotent
    @PatchMapping("/read-all")
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
