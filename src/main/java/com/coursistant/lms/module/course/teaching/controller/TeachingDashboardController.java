package com.coursistant.lms.module.course.teaching.controller;

import com.coursistant.lms.module.course.teaching.dto.TeachingActivityResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingCourseResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingDeadlineResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingGradingQueueItemResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingRecentActivityResponse;
import com.coursistant.lms.module.course.teaching.service.TeachingDashboardService;
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
@RequestMapping("/v2/me/teaching")
public class TeachingDashboardController {

    @Resource
    private TeachingDashboardService teachingDashboardService;

    @GetMapping("/courses")
    public ApiResponse<List<TeachingCourseResponse>> courses(HttpServletRequest request) {
        return ApiResponse.success(teachingDashboardService.listCourses(currentUserId(request)));
    }

    @GetMapping("/grading-queue")
    public ApiResponse<List<TeachingGradingQueueItemResponse>> gradingQueue(HttpServletRequest request) {
        return ApiResponse.success(teachingDashboardService.listGradingQueue(currentUserId(request)));
    }

    @GetMapping("/activities/upcoming")
    public ApiResponse<List<TeachingActivityResponse>> activities(HttpServletRequest request,
                                                                  @RequestParam(required = false) Integer days) {
        return ApiResponse.success(
                teachingDashboardService.listUpcomingActivities(currentUserId(request), days));
    }

    @GetMapping("/deadlines/upcoming")
    public ApiResponse<List<TeachingDeadlineResponse>> deadlines(HttpServletRequest request,
                                                                 @RequestParam(required = false) Integer days) {
        return ApiResponse.success(
                teachingDashboardService.listUpcomingDeadlines(currentUserId(request), days));
    }

    @GetMapping("/activity/recent")
    public ApiResponse<List<TeachingRecentActivityResponse>> recentActivity(
            HttpServletRequest request,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
                teachingDashboardService.listRecentActivity(currentUserId(request), limit));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
