package com.coursistant.lms.module.course.announcement.controller;

import com.coursistant.lms.module.course.announcement.dto.RecentAnnouncementResponse;
import com.coursistant.lms.module.course.announcement.service.CourseAnnouncementService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/me/announcements")
@Tag(name = "MeAnnouncements", description = "Current-user recent announcements across courses")
public class MeAnnouncementController {

    @Resource
    private CourseAnnouncementService courseAnnouncementService;

    @GetMapping("/recent")
    @Operation(operationId = "meAnnouncementsRecent", summary = "List my recent announcements")
    public ApiResponse<List<RecentAnnouncementResponse>> recent(HttpServletRequest request,
                                                                @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(courseAnnouncementService.listRecentForUser(currentUserId(request), limit));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
