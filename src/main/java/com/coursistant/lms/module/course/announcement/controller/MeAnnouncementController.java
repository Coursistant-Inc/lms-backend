package com.coursistant.lms.module.course.announcement.controller;

import com.coursistant.lms.module.course.announcement.dto.AnnouncementSummaryResponse;
import com.coursistant.lms.module.course.announcement.service.CourseAnnouncementService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/me/announcements")
public class MeAnnouncementController {

    @Resource
    private CourseAnnouncementService courseAnnouncementService;

    @GetMapping("/recent")
    public ApiResponse<List<AnnouncementSummaryResponse>> recent(HttpServletRequest request,
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
