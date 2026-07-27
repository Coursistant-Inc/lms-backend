package com.coursistant.lms.module.course.event.controller;

import com.coursistant.lms.module.course.event.dto.UpcomingCourseActivityResponse;
import com.coursistant.lms.module.course.event.service.CourseEventService;
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
@RequestMapping("/v2/me/events")
public class MeCourseEventController {

    @Resource
    private CourseEventService courseEventService;

    @GetMapping("/upcoming")
    public ApiResponse<List<UpcomingCourseActivityResponse>> upcoming(HttpServletRequest request,
                                                                      @RequestParam(required = false) Integer days) {
        return ApiResponse.success(
                courseEventService.listUpcomingActivitiesForUser(currentUserId(request), days));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
