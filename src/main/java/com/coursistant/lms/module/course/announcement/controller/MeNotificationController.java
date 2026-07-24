package com.coursistant.lms.module.course.announcement.controller;

import com.coursistant.lms.module.course.announcement.dto.NotificationResponse;
import com.coursistant.lms.module.course.announcement.service.AnnouncementNotificationService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/me/notifications")
public class MeNotificationController {

    @Resource
    private AnnouncementNotificationService announcementNotificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(HttpServletRequest request,
                                                        @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(announcementNotificationService.listForUser(currentUserId(request), limit));
    }

    @Idempotent
    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(HttpServletRequest request, @PathVariable Integer notificationId) {
        announcementNotificationService.markRead(currentUserId(request), notificationId);
        return ApiResponse.success();
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
